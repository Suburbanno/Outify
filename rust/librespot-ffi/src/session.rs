use crate::{ANDROID_CLIENT_ID, OUTIFY_CLIENT_ID, TOKIO_RUNTIME};
use jni::{objects::JClass, sys::JNIEnv};
use librespot_core::{Session, SessionConfig, authentication::Credentials};
use once_cell::sync::OnceCell;

pub static SESSION: OnceCell<Session> = OnceCell::new();

// Initializes the session work further usage
pub async fn initialize_session() {
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
        client_id: ANDROID_CLIENT_ID.to_owned(),
        ..Default::default()
    };
    let session = Session::with_handle(session_config, None, handle);
    if SESSION.set(session).is_err() {
        log::warn!("Session was concurrently set!");
        return;
    }

    debug!("Session initialized!");
}

// Connects the already initialized session
pub async fn connect(credentials: Credentials) -> Result<Session, librespot_core::Error> {
    let session = SESSION.get()
        .ok_or_else(|| {
            warn!("Attempted to connect session, but session isn't initialized!'");
            librespot_core::Error::internal(format!("Attempted to connect session, but session isn't initialized!'"))
        })?;

    session.connect(credentials, false).await.map_err(|e| {
        error!("Session failed to connect: {e}");
        e
    })?;

    info!("Session connected!");
    Ok(session.clone())
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_Session_initializeSession(
    _env: JNIEnv,
    _this: JClass,
) {
    let rt = match TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            warn!("Failed to initialize session as Tokio Runtime is not initialized!");
            return;
        }
    };

    rt.block_on(async {
        initialize_session().await;
    });
}
