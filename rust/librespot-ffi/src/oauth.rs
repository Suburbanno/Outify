use crate::TOKIO_RUNTIME;

use jni::{
    JNIEnv,
    objects::{JClass, JObject, JString, JValue},
    sys::{jboolean, jobject, jstring},
};
use once_cell::sync::OnceCell;
use std::sync::Mutex;

static OAUTH_SESSION: OnceCell<Mutex<OAuthSession>> = OnceCell::new();

use librespot_oauth::{OAuthClient, OAuthClientBuilder, OAuthToken};
use oauth2::{AuthorizationCode, PkceCodeVerifier, url::Url};

use librespot_core::Error;

use crate::{SPOTIFY_CALLBACK_URI, OUTIFY_CLIENT_ID, SCOPES};

pub struct OAuthSession {
    client: OAuthClient,
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
    pub fn new(client_id: &str, redirect_uri: &str, scopes: &[&str]) -> Result<Self, Error> {
        let client = OAuthClientBuilder::new(client_id, redirect_uri, scopes.to_vec())
            .build()
            .map_err(|e| Error::internal(format!("Unable to build OAuth client: {e}")))?;
        let (auth_url, pkce_verifier) = client.set_auth_url();

        Ok(Self {
            client,
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

        info!("get_access_token: token_response: {:#?}", token_response);

        // Refreshing token
        let refresh_token = token_response.refresh_token.clone();
        let refreshed = self
            .client
            .refresh_token_async(&refresh_token)
            .await
            .map_err(|e| Error::unknown(format!("Unable to refresh OAuth token: {e}")))?;

        println!("Refreshed OAuth Token: {:#?}", refreshed);

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

// Initializes the OAuth Session.
// TODO: Make the parameters do the actual work
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_SpAuthManager_initialize(
    _env: JNIEnv,
    _this: JObject,
    _client_id: jstring,
    _redirect_uri: jstring,
    _scopes: jstring,
) -> jboolean {
    let redirect = format!("{}/verify", SPOTIFY_CALLBACK_URI);

    let session = OAuthSession::new(OUTIFY_CLIENT_ID, &redirect, SCOPES).unwrap();
    OAUTH_SESSION.set(Mutex::new(session)).ok();
    1
}

/// oAuth Get Auth URL
/// Retrieves the authorization URL, where the user has to authorize
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_SpAuthManager_getAuthorizationURL(
    env: JNIEnv,
    _this: JObject,
) -> jstring {
    let rt = TOKIO_RUNTIME
        .get()
        .expect("Tokio runtime is not initialized!");

    let auth_url = rt.block_on(async {
        let session_mutex = OAUTH_SESSION
            .get()
            .expect("OAuth session is not initialized!");
        let session = session_mutex.lock().unwrap();
        session.auth_url().clone()
    });

    env.new_string(auth_url.to_string()).unwrap().into_raw()
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
        let mut session = session_mutex.lock().unwrap();
        session.get_access_token(code).await
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

// Refreshes the authentication token using refresh token.
//
// Returns an object - TokenResponseDto
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_SpAuthManager_refreshToken(
    mut env: JNIEnv,
    _this: JClass,
    access_token: JString,
    refresh_token: JString,
) -> jobject {
    // helper function for returning java errors
    let return_error =
        |env: &mut JNIEnv, msg: String| -> jobject { make_error_token_response_dto(env, msg) };

    let refresh = match env.get_string(&refresh_token) {
        Ok(r) => r.into(),
        Err(e) => {
            return return_error(
                &mut env,
                format!("JNI: get_string failed for refresh_token: {}", e),
            );
        }
    };

    let rt = match TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            return return_error(&mut env, format!("Tokio runtime is not initialized!"));
        }
    };

    let result = match rt.block_on(async {
        let session_mutex = OAUTH_SESSION
            .get()
            .expect("OAuth session is not initialized!");
        let mut session = session_mutex.lock().unwrap();
        session.refresh_token(refresh).await
    }) {
        Ok(dto) => token_response_dto_as_jobject(
            env,
            TokenResponseDto {
                access_token: dto.access_token,
                refresh_token: dto.refresh_token,
                expires_at: dto.expires_at,
                error: None,
            },
        ),
        Err(e) => return return_error(&mut env, format!("Refresh token failed with: {}", e)),
    };

    result
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
