use jni::{
    JNIEnv,
    objects::{JClass, JObject, JString},
    sys::{jboolean, jstring},
};
use librespot_core::authentication::Credentials;
use oauth2::url::Url;

use crate::session::with_session;

// Checks for cached credentials. Does NOT use Session
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_AuthManager_hasCachedCredentials(
    _env: JNIEnv,
    _this: JClass,
) -> jboolean {
    let dir = match crate::FILES_DIR.get() {
        Some(d) => d,
        None => {
            warn!("Cannot check for cached credentials as FILES_DIR is not set.");
            return 0;
        }
    };

    dir.join("credentials.json").is_file() as jboolean
}

// Returns the auth URL for OAuth flow
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_AuthManager_getAuthorizationURL(
    env: JNIEnv,
    _this: JObject,
) -> jstring {
    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            warn!("Cannot get auth url as tokio isn't initialized'");
            return std::ptr::null_mut();
        }
    };

    let auth_url_opt: Option<Url> = match with_session(|session| {
        rt.block_on(async move {
            match crate::oauth::setup_oauth_session(&session) {
                Some(mutex) => {
                    let guard = mutex.lock().await;
                    Some(guard.auth_url().clone())
                }
                None => None,
            }
        })
    }) {
        Ok(val) => val,
        Err(e) => {
            error!("Failed to get session: {}", e);
            None 
        }
    };

    let auth_url = match auth_url_opt {
        Some(u) => u,
        None => {
            warn!("OAuth Session failed to setup!");
            return std::ptr::null_mut();
        }
    };

    match env.new_string(auth_url.to_string()) {
        Ok(java_str) => java_str.into_raw(),
        Err(e) => {
            warn!("Failed to create Java string: {:?}", e);
            std::ptr::null_mut()
        }
    }
}

// Called after OAuth flow.
// Caches the credentials.
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_AuthManager_handleOAuthCode(
    mut env: JNIEnv,
    _this: JObject,
    code: JString,
    _state: JString,
) -> jboolean {
    // Carefully extract the code string once so we don't hold JNI references across await points.
    let code: String = match env.get_string(&code) {
        Ok(js) => js.into(),
        Err(e) => {
            error!("JNI failed to read code: {}", e);
            return false as jboolean;
        }
    };

    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(rt) => rt,
        None => {
            error!("JNI: Tokio runtime is not initialized!");
            return false as jboolean;
        }
    };

    let session_mutex = match crate::oauth::OAUTH_SESSION.get() {
        Some(m) => m,
        None => {
            error!("OAuth Sessio is not initialized!");
            return false as jboolean;
        }
    };

    let token = match rt.block_on(async {
        let mut guard = session_mutex.lock().await;
        guard.get_access_token(code).await
    }) {
        Ok(tok) => tok,
        Err(e) => {
            error!("OAuth error: {}", e);
            return false as jboolean;
        }
    };

    // Saving credentials if session cache exists
    if let Err(e) = rt.block_on(async {
        let guard = session_mutex.lock().await;

        if let Some(cache) = guard.session.cache() {
            let cred = Credentials::with_access_token(&token.access_token);
            cache.save_credentials(&cred);
        } else {
            warn!("Session has no cache, cannot persist credentials");
        }

        Ok::<(), ()>(())
    }) {
        warn!("Could not save credentials to cache: {:?}", e);
    }

    1 as jboolean
}
