use std::{path::PathBuf, sync::RwLock};

use crate::{CACHE_DIR, FILES_DIR, TOKIO_RUNTIME};
use jni::{
    objects::{GlobalRef, JClass, JObject},
    sys::JNIEnv,
};
use librespot_core::{Session, SessionConfig, cache::Cache, config::KEYMASTER_CLIENT_ID};
use once_cell::sync::OnceCell;

pub static SESSION: OnceCell<RwLock<Option<Session>>> = OnceCell::new();
pub static SESSION_CALLBACK: OnceCell<RwLock<Option<GlobalRef>>> = OnceCell::new();

pub fn init_session_container() {
    SESSION.get_or_init(|| RwLock::new(None));
    SESSION_CALLBACK.get_or_init(|| RwLock::new(None));
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
    *guard = Some(session.clone());

    start_shutdown_listener(session);
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

    debug!("Session connected!");
    Ok(session.clone())
}

// Listens for session shutdowns
fn start_shutdown_listener(session: Session) {
    let rt = match TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            warn!("Failed to start session shutdown listener as Tokio Runtime is not initialized!");
            return;
        }
    };

    rt.handle().spawn(async move {
        let mut shutdown_rx = session.subscribe_shutdown();
        shutdown_rx.changed().await.ok();
        notify_callback("onShutdown".to_string());
        cleanup();

        warn!("Session shutdown! Auto-restarting..");

        initialize_session().await;
        crate::spirc::initialize_spirc().await;
    });
}

fn notify_callback(method: String) {
    let jvm = match crate::JVM.get() {
        Some(j) => j,
        None => {
            error!("Cannot call session callback: JVM not initialized");
            return;
        }
    };

    let mut env = match jvm.attach_current_thread() {
        Ok(e) => e,
        Err(e) => {
            error!("Failed to attach current thread: {e}");
            return;
        }
    };

    if let Some(lock) = SESSION_CALLBACK.get() {
        let guard = lock.read().unwrap();

        if let Some(callback) = &*guard {
            env.call_method(callback.as_obj(), method, "()V", &[]).ok();
        }
    }
}

// Sets the SessionCallback
pub fn set_session_callback(global: GlobalRef) {
    if let Some(lock) = SESSION_CALLBACK.get() {
        let mut guard = lock.write().unwrap();
        *guard = Some(global);
    }
}

async fn cleanup() {
    if let Some(lock) = SESSION.get() {
        let mut guard = lock.write().unwrap();
        *guard = None;
    }

    crate::spirc::with_spirc(|spirc| {
        spirc.cleanup();
    });
}

// Helper function to retrieve &Session
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
