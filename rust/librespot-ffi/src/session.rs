use std::path::PathBuf;

use crate::{CACHE_DIR, FILES_DIR, TOKIO_RUNTIME};
use jni::{objects::JClass, sys::JNIEnv};
use librespot_core::{
    Session, SessionConfig, cache::Cache, config::KEYMASTER_CLIENT_ID,
};
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

    // Geting session cache dir
    let os_cache_dir: PathBuf = CACHE_DIR
        .get()
        .expect("Failed to get Cache Dir!")
        .to_path_buf();
    let os_files_dir: PathBuf = FILES_DIR
        .get()
        .expect("Failed to get Files Dir!")
        .to_path_buf();
    let cache: Cache = Cache::new(
        Some(&os_files_dir),
        None,
        Some(&os_cache_dir),
        None,
    )
    .unwrap();
    trace!("Initialized new cache!");

    let handle = rt.handle().clone();
    let session_config = SessionConfig {
        client_id: KEYMASTER_CLIENT_ID.to_owned(),
        ..Default::default()
    };
    let session = Session::with_handle(session_config, Some(cache), handle);
    if SESSION.set(session).is_err() {
        warn!("Session was concurrently set!");
        return;
    }

    debug!("Session initialized!");
}

// Connects the already initialized session
pub async fn connect() -> Result<Session, librespot_core::Error> {
    let session = SESSION.get().ok_or_else(|| {
        warn!("Attempted to connect session, but session isn't initialized!'");
        librespot_core::Error::internal(format!(
            "Attempted to connect session, but session isn't initialized!'"
        ))
    })?;

    let credentials = session
        .cache()
        .and_then(|cache| cache.credentials())
        .ok_or_else(|| {
            warn!("No cached credentials available for session");
            librespot_core::Error::unauthenticated("No cached credentials available".to_string())
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
