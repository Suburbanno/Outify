use std::{path::PathBuf, sync::RwLock};

use crate::{CACHE_DIR, FILES_DIR, TOKIO_RUNTIME};
use jni::{
    objects::{JClass, JObject},
    sys::JNIEnv,
};
use librespot_core::{Session, SessionConfig, cache::Cache, config::KEYMASTER_CLIENT_ID};
use once_cell::sync::OnceCell;

pub static SESSION: OnceCell<RwLock<Option<Session>>> = OnceCell::new();

pub fn init_session_container() {
    SESSION.get_or_init(|| RwLock::new(None));
}

// Initializes the session work further usage
pub async fn initialize_session() {
    let container = SESSION.get().expect("Session container not initialized");

    {
        let guard = container.read().unwrap();
        if guard.is_some() {
            warn!("Session already exists!");
            return;
        }
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
    let cache: Cache = Cache::new(Some(&os_files_dir), None, Some(&os_cache_dir), None).unwrap();
    trace!("Initialized new cache!");

    let handle = rt.handle().clone();
    let session_config = SessionConfig {
        client_id: KEYMASTER_CLIENT_ID.to_owned(),
        ..Default::default()
    };
    let session = Session::with_handle(session_config, Some(cache), handle);

    let mut guard = container.write().unwrap();
    *guard = Some(session);

    debug!("Session initialized!");
}

// Connects the already initialized session
pub async fn connect() -> Result<Session, librespot_core::Error> {
    let session = match with_session(|s| s.clone()) {
        Ok(s) => s,
        Err(e) => {
            error!("Failed to get session: {}", e);
            return Err(librespot_core::Error::internal("Failed to get session"));
        }
    };

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
    start_shutdown_listener(session.clone());
    info!("session id: {}", session.session_id());
    info!("session con: {}", session.connection_id());
    Ok(session.clone())
}

fn start_shutdown_listener(session: Session) {
    tokio::spawn(async move {
        let mut shutdown_rx = session.subscribe_shutdown();
        shutdown_rx.changed().await.ok();

        warn!("Session shutdown!");
    });
}

pub fn with_session<F, R>(f: F) -> Result<R, librespot_core::Error>
where
    F: FnOnce(&Session) -> R,
{
    let container = SESSION
        .get()
        .ok_or_else(|| librespot_core::Error::internal("Session container not initialized"))?;

    let guard = container.read().unwrap();

    let session = guard
        .as_ref()
        .ok_or_else(|| librespot_core::Error::internal("Session not created"))?;

    Ok(f(session))
}
