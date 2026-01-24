use crate::{CACHE_DIR, FILES_DIR, TOKIO_RUNTIME};
use std::path::PathBuf;

use jni::objects::{JObject, JString, JValue};
use jni::sys::jstring;
use jni::{JNIEnv, sys::jobject};
use librespot_core::{Error, Session, authentication::Credentials, cache::Cache};
use librespot_oauth::{OAuthClient, OAuthClientBuilder, OAuthToken};
use oauth2::{AuthorizationCode, PkceCodeVerifier, url::Url};
use once_cell::sync::OnceCell;
use tokio::sync::Mutex;

pub const SPOTIFY_CALLBACK_URI: &str = "http://127.0.0.1:5588/login";
pub const SCOPES: &[&str] = &[
    "streaming",
    "user-read-playback-state",
    "user-modify-playback-state",
    "user-read-currently-playing",
];

static CREDENTIALS: once_cell::sync::Lazy<Mutex<Option<Credentials>>> =
    once_cell::sync::Lazy::new(|| Mutex::new(None));
static OAUTH_SESSION: OnceCell<Mutex<OAuthSession>> = OnceCell::new();

struct OAuthSession {
    client: OAuthClient,
    session: Session,
    pub pkce_verifier: Option<PkceCodeVerifier>,
    pub auth_url: Url,
}

/// Contains token related data, we care about
pub struct TokenResponseDto {
    pub access_token: String,
    pub refresh_token: String,
    // Nanoseconds remaining till expiration
    pub expires_at: i64,
    pub error: Option<String>,
}

impl OAuthSession {
    pub fn new(session: &Session, redirect_uri: &str, scopes: &[&str]) -> Result<Self, Error> {
        let client_id = session.client_id();
        let client = OAuthClientBuilder::new(client_id.as_str(), redirect_uri, scopes.to_vec())
            .build()
            .map_err(|e| Error::internal(format!("Unable to build OAuth client: {e}")))?;
        let (auth_url, pkce_verifier) = client.set_auth_url();

        Ok(Self {
            client,
            session: session.clone(),
            pkce_verifier: Some(pkce_verifier),
            auth_url,
        })
    }

    pub fn auth_url(&self) -> &Url {
        &self.auth_url
    }

    /// Retrieves the Access Token.
    /// Automatically refreshes it.
    pub async fn get_access_token(&mut self, code: String) -> Result<OAuthToken, Error> {
        let pkce_verifier = self
            .pkce_verifier
            .take()
            .ok_or(Error::internal(format!("Missing Pkce Verifier")))?;

        let auth_code = AuthorizationCode::new(code);

        let token_response = self
            .client
            .get_access_token_with_verifier_async(pkce_verifier, auth_code)
            .await
            .map_err(|e| Error::unavailable(format!("Unable to get OAuth token: {e}")))?;

        // Refreshing token
        let refresh_token = token_response.refresh_token.clone();
        let refreshed = self
            .client
            .refresh_token_async(&refresh_token)
            .await
            .map_err(|e| Error::unknown(format!("Unable to refresh OAuth token: {e}")))?;

        Ok(token_response)
    }

    /// Refreshes the auth token and retrieves a new one
    pub async fn refresh_token(
        &mut self,
        refresh_token: String,
    ) -> Result<TokenResponseDto, Error> {
        let refreshed = self
            .client
            .refresh_token_async(&refresh_token)
            .await
            .map_err(|e| Error::unauthenticated(format!("Unable to refresh OAuth token: {e}")))?;

        debug!("Refreshed OAuth token!");
        let expiration = refreshed
            .expires_at
            .saturating_duration_since(std::time::Instant::now())
            .as_nanos();

        let expiration_nanos = if expiration > i64::MAX as u128 {
            i64::MAX
        } else {
            expiration as i64
        };

        Ok(TokenResponseDto {
            access_token: refreshed.access_token,
            refresh_token: refreshed.refresh_token,
            expires_at: expiration_nanos,
            error: None,
        })
    }
}

// Returns the auth URL for OAuth flow
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_SpAuthManager_getAuthorizationURL(
    env: JNIEnv,
    _this: JObject,
) -> jstring {
    let rt = match TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            warn!("Cannot get auth url as tokio isn't initialized'");
            return std::ptr::null_mut();
        }
    };

    let session = match crate::session::SESSION.get() {
        Some(s) => s,
        None => {
            warn!("Cannot get auth url as Session isn't initialized");
            return std::ptr::null_mut();
        }
    };

    let auth_url_opt: Option<Url> = rt.block_on(async {
        match setup_oauth_session(session) {
            Some(mutex) => {
                // Obtain async lock and clone auth_url
                let guard = mutex.lock().await;
                Some(guard.auth_url().clone())
            }
            None => None,
        }
    });

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

// Retrieves access, refresh token and token expiration.
//
// Returns as an object - TokenResponseDto
// TODO: Implement state for CSRF
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_SpAuthManager_getTokenData(
    mut env: JNIEnv,
    _this: JObject,
    code: JString,
    _state: JString,
) -> jobject {
    // helper function for returning java errors
    let return_error =
        |env: &mut JNIEnv, msg: String| -> jobject { make_error_token_response_dto(env, msg) };

    let code: String = match env.get_string(&code) {
        Ok(js) => js.into(),
        Err(e) => return return_error(&mut env, format!("JNI: failed to read code: {}", e)),
    };

    let rt = match TOKIO_RUNTIME.get() {
        Some(rt) => rt,
        None => return return_error(&mut env, format!("JNI: Tokio runtime is not initialized!")),
    };

    let session_mutex = match OAUTH_SESSION.get() {
        Some(m) => m,
        None => {
            return return_error(&mut env, "OAuth session is not initialized!".to_string());
        }
    };

    let token = match rt.block_on(async {
        // lock the async mutex
        let mut guard = session_mutex.lock().await;
        // call the async method that needs &mut self
        guard.get_access_token(code).await
    }) {
        Ok(tok) => tok,
        Err(e) => {
            return return_error(&mut env, format!("OAuth error: {}", e));
        }
    };

    // Converting instant into remaining nano seconds till JWT token expires
    let expiraton: i64 = token
        .expires_at
        .saturating_duration_since(std::time::Instant::now())
        .as_nanos() as i64;

    let dto = TokenResponseDto {
        access_token: token.access_token,
        refresh_token: token.refresh_token,
        expires_at: expiraton,
        error: None,
    };

    token_response_dto_as_jobject(env, dto)
}

// If we already have cached credentials, we will use them, otherwise we will follow OAuth
pub async fn login() {
    // Try cached credentials
    if let Some(cred) = get_cached_credentials() {
        let mut guard = CREDENTIALS.lock().await;
        *guard = Some(cred);
        debug!("Reusing cached credentials");
        crate::CONNECTING.store(true, std::sync::atomic::Ordering::SeqCst);
        return;
    }

    debug!("Getting credentials from OAuth");

    if crate::session::SESSION.get().is_none() {
        error!("Cannot login using OAuth, because session isn't initialized!");
        return;
    }

    let session = crate::session::SESSION.get().unwrap();

    let osession = match setup_oauth_session(&session) {
        Some(s) => s,
        None => {
            warn!("Failed to setup oauth session");
            return;
        }
    };

    crate::CONNECTING.store(false, std::sync::atomic::Ordering::SeqCst);
}

fn setup_oauth_session(session: &Session) -> Option<&'static Mutex<OAuthSession>> {
    debug!("Setting up OAuthSession");
    if let Some(existing) = OAUTH_SESSION.get() {
        debug!("OAuthSession already initialized!");
        return Some(existing);
    }

    let osession = match OAuthSession::new(&session, &SPOTIFY_CALLBACK_URI, SCOPES) {
        Ok(s) => s,
        Err(e) => {
            error!("OAuth session setup failed with: {}", e);
            return None;
        }
    };

    if OAUTH_SESSION.set(Mutex::new(osession)).is_err() {
        warn!("Failed to set OAuth Session concurrently!");
    }

    OAUTH_SESSION.get()
}

// Retrieves cached credentials if possible
fn get_cached_credentials() -> Option<Credentials> {
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
        Some(&os_cache_dir),
        Some(&os_cache_dir),
        None,
    )
    .unwrap();
    cache.credentials()
}

// Converts the TokenResponseDto struct into Java Object
fn token_response_dto_as_jobject(mut env: JNIEnv, dto: TokenResponseDto) -> jobject {
    // helper function for returning java errors
    let return_error =
        |env: &mut JNIEnv, msg: String| -> jobject { make_error_token_response_dto(env, msg) };

    // Encapsulating as TokenResponseDto
    let class = match env.find_class("cc/tomko/outify/core/auth/TokenResponseDto") {
        Ok(c) => c,
        Err(e) => {
            return return_error(
                &mut env,
                format!("JNI: find_class failed for TokenResponseDto: {}", e),
            );
        }
    };

    let jaccess = match env.new_string(dto.access_token) {
        Ok(a) => a.into(),
        Err(e) => {
            return return_error(&mut env, format!("JNI: new_string failed: {}", e));
        }
    };

    let jrefresh = match env.new_string(dto.refresh_token) {
        Ok(a) => a.into(),
        Err(e) => {
            return return_error(&mut env, format!("JNI: new_string failed: {}", e));
        }
    };

    let jerror: JObject = if let Some(err) = dto.error {
        match env.new_string(err) {
            Ok(s) => s.into(),
            Err(e) => {
                return return_error(&mut env, format!("JNI: new_string failed: {}", e));
            }
        }
    } else {
        JObject::null()
    };

    let values = &[
        JValue::Object(&jaccess),
        JValue::Object(&jrefresh),
        JValue::Long(dto.expires_at),
        JValue::Object(&jerror),
    ];

    let ctor_sig = "(Ljava/lang/String;Ljava/lang/String;JLjava/lang/String;)V";
    let obj = match env.new_object(class, ctor_sig, values) {
        Ok(o) => o,
        Err(e) => {
            return return_error(&mut env, format!("JNI: new_object failed: {}", e));
        }
    };

    obj.into_raw()
}

fn make_error_token_response_dto(env: &mut JNIEnv, error: String) -> jobject {
    // Encapsulating as TokenResponseDto
    let class = match env.find_class("cc/tomko/outify/core/auth/TokenResponseDto") {
        Ok(c) => c,
        Err(_) => return JObject::null().into_raw(), // fail-safe
    };

    let j_access = JObject::null();
    let j_refresh = JObject::null();
    let j_expiration = JValue::Long(0);
    let j_error = match env.new_string(error) {
        Ok(s) => s.into(),
        Err(_) => JObject::null(),
    };

    let ctor_sig = "(Ljava/lang/String;Ljava/lang/String;JLjava/lang/String;)V";
    env.new_object(
        class,
        ctor_sig,
        &[
            JValue::Object(&j_access),
            JValue::Object(&j_refresh),
            j_expiration,
            JValue::Object(&j_error),
        ],
    )
    .unwrap_or_else(|_| JObject::null())
    .into_raw()
}
