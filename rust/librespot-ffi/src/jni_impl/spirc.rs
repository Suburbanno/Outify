use jni::{JNIEnv, objects::JClass};
use librespot_core::{Session, authentication::Credentials};

use crate::spirc::SpircRuntime;

// Initializes the [SpircRuntime] into OnceCell.
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_spirc_Spirc_initializeSpirc(
    env: JNIEnv,
    _this: JClass,
) {
    debug!("Initializing SpircRuntime");

    if !super::SPIRC_RUNTIME.get().is_none() {
        warn!("SpircRuntime already initialized!");
        return;
    }

    let session = match crate::session::SESSION.get() {
        Some(s) => s,
        None => {
            error!("Cannot initialize SpircRuntime as SESSION isn't initialized!");
            return;
        }
    };

    if session.cache().is_none() {
        error!("Cannot initialize SpircRuntime as session cache is none!");
        return;
    }

    let cache = session.cache().unwrap();
    let credentials = match cache.credentials() {
        Some(c) => c,
        None => {
            error!("Cannot initialize SpircRuntime as cached credentials are None!");
            return;
        }
    };

    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            error!("TOKIO_RUNTIME isn't initialized in SpircRuntime init!'");
            return;
        }
    };

    rt.block_on(async {
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
    })
}


