use jni::{
    JNIEnv,
    objects::{JClass, JString},
    sys::jboolean,
};
use librespot_connect::LoadRequest;

use crate::spirc::SpircRuntime;

// Initializes the [SpircRuntime] into OnceCell.
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_spirc_Spirc_initializeSpirc(
    env: JNIEnv,
    this: JClass,
) -> jboolean {
    debug!("Initializing SpircRuntime");

    if !super::SPIRC_RUNTIME.get().is_none() {
        warn!("SpircRuntime already initialized!");
        return 0 as jboolean;
    }

    let session = match crate::session::SESSION.get() {
        Some(s) => s,
        None => {
            error!("Cannot initialize SpircRuntime as SESSION isn't initialized!");
            return 0 as jboolean;
        }
    };

    if session.cache().is_none() {
        error!("Cannot initialize SpircRuntime as session cache is none!");
        return 0 as jboolean;
    }

    let cache = session.cache().unwrap();
    let credentials = match cache.credentials() {
        Some(c) => c,
        None => {
            error!("Cannot initialize SpircRuntime as cached credentials are None!");
            return 0 as jboolean;
        }
    };

    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            error!("TOKIO_RUNTIME isn't initialized in SpircRuntime (jni_impl)!'");
            return 0 as jboolean;
        }
    };

    let global_this = match env.new_global_ref(this) {
        Ok(r) => r,
        Err(e) => {
            error!("Failed to create GlobalRef: {}", e);
            return 0 as jboolean;
        }
    };

    let jvm = crate::JVM.get().expect("JVM not initialized!").clone();
    rt.spawn(async move {
        let runtime = match SpircRuntime::new(&session, credentials).await {
            Ok(r) => r,
            Err(e) => {
                error!("Failed to create SpircRuntime with err: {}", e);
                return;
            }
        };

        if super::SPIRC_RUNTIME.set(runtime).is_err() {
            warn!("Cannot set SPIRC_RUNTIME concurrently!");
            return;
        }

        debug!("SpircRuntime initialized");

        let mut env = match jvm.attach_current_thread() {
            Ok(e) => e,
            Err(e) => {
                error!("Failed to attach JVM to thread: {}", e);
                return;
            }
        };

        // Calling callback
        if let Err(e) = env.call_method(global_this, "onSpircInitialized", "()V", &[]) {
            error!("Failed to call onSpircInitialized callback: {}", e);
        }
    });

    1 as jboolean
}

// Loads a Spotify URI specified
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_spirc_Spirc_load(
    mut env: JNIEnv,
    _this: JClass,
    juri: JString,
) -> jboolean {
    let runtime = match super::SPIRC_RUNTIME.get() {
        Some(r) => r,
        None => {
            warn!("Spirc not initialized for load");
            return 0;
        }
    };

    let uri = match env.get_string(&juri) {
        Ok(u) => u.into(),
        Err(e) => {
            warn!("Failed to get URI from JNI juri: {}", e);
            return 0;
        }
    };

    let req = LoadRequest::from_context_uri(
        uri,
        librespot_connect::LoadRequestOptions {
            ..Default::default()
        },
    );

    match runtime.load(req) {
        Ok(_) => {}
        Err(e) => {
            warn!("Failed to Spirc load: {}", e);
        }
    }

    1
}

// Activates the Spirc session
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_spirc_Spirc_activate(
    _env: JNIEnv,
    _this: JClass,
) -> jboolean {
    let runtime = match super::SPIRC_RUNTIME.get() {
        Some(r) => r,
        None => {
            warn!("Spirc not initialized for activation");
            return 0;
        }
    };

    match runtime.activate() {
        Ok(_) => {}
        Err(e) => {
            error!("Failed to activate Spirc: {}", e);
            return 0 as jboolean;
        }
    }

    1 as jboolean
}

// Transfers the Spirc session to us
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_spirc_Spirc_transfer(
    _env: JNIEnv,
    _this: JClass,
) -> jboolean {
    let runtime = match super::SPIRC_RUNTIME.get() {
        Some(r) => r,
        None => {
            warn!("Spirc not initialized for transfer");
            return 0;
        }
    };

    match runtime.transfer() {
        Ok(_) => {}
        Err(e) => {
            error!("Failed to activate Spirc: {}", e);
            return 0 as jboolean;
        }
    }

    1 as jboolean
}
