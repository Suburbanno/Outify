use crate::{OUTIFY_CLIENT_ID, TOKIO_RUNTIME};
use jni::{objects::JClass, sys::JNIEnv};
use librespot_core::{Session, SessionConfig};
use once_cell::sync::OnceCell;

pub static SESSION: OnceCell<Session> = OnceCell::new();

// Initializes the session work further usage
pub fn initialize_session() {
    if SESSION.get().is_some() {
        warn!("Session is already initialized!");
        return;
    }

    let rt = match TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            warn!("Failed to initialize session as Tokio Runtime is not initialized!");
            return;
        }
    };

    let handle = rt.handle().clone();
    let session_config = SessionConfig {
        client_id: OUTIFY_CLIENT_ID.to_owned(),
        ..Default::default()
    };
    let session = Session::with_handle(session_config, None, handle);
    if SESSION.set(session).is_err() {
        log::warn!("Session was concurrently set!");
        return;
    }

    debug!("Session initialized!");
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_Session_initializeSession(
    _env: JNIEnv,
    _this: JClass,
) {
    initialize_session();
}
