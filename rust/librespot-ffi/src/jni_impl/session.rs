use jni::{
    JNIEnv,
    objects::{GlobalRef, JClass, JObject},
    sys::jboolean,
};
use once_cell::sync::OnceCell;

use crate::session::with_session;

static SESSION_CONNECTED_CALLBACK: OnceCell<GlobalRef> = OnceCell::new();

#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_Session_initializeSession(
    env: JNIEnv,
    _this: JClass,
    callback: JObject, // <-- Kotlin callback passed in
) -> jboolean {
    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => return 0,
    };

    // Make a GlobalRef so it can be safely used across threads
    let callback_global = match env.new_global_ref(callback) {
        Ok(c) => c,
        Err(_) => return 0,
    };

    let _ = SESSION_CONNECTED_CALLBACK.set(callback_global);

    let handle = rt.handle().clone();
    let jvm = crate::JVM.get().unwrap();

    handle.spawn(async move {
        crate::session::initialize_session().await;
        if let Ok(_) = crate::session::connect().await {
            // Call the Kotlin callback
            if let Some(cb) = SESSION_CONNECTED_CALLBACK.get() {
                let mut env = jvm.attach_current_thread().unwrap();
                let _ = env.call_method(cb.as_obj(), "run", "()V", &[]);
            }

            // let max_wait = std::time::Duration::from_secs(10);
            // let start = std::time::Instant::now();
            // loop {
            //     // Access the session copy from the container
            //     if let Ok(session) = crate::session::with_session(|s| s.clone()) {
            //         if !session.connection_id().is_empty() {
            //             break; // session ready
            //         }
            //     } else {
            //         // session container missing — log and break to avoid infinite loop
            //         error!("Session container missing while waiting for connection");
            //         break;
            //     }
            //
            //     if start.elapsed() > max_wait {
            //         error!("Timed out waiting for session connection_id to be set");
            //         break;
            //     }
            //     tokio::time::sleep(std::time::Duration::from_millis(100)).await;
            // }
            //
            // crate::spirc::initialize_spirc().await;
        }
    });

    1
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_Session_shutdown(
    _env: JNIEnv,
    _this: JClass,
) -> jboolean {
    with_session(|session| session.shutdown());
    1
}
