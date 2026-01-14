use crate::TOKIO_RUNTIME;
#[allow(unused_imports)]
use crate::{logd, loge, logi, logv}; // Exporting logger macros

use jni::{
    JNIEnv,
    objects::{JClass, JObject, JString},
    sys::{jboolean, jobjectArray, jstring},
};
use once_cell::sync::OnceCell;
use std::sync::Mutex;

static OAUTH_SESSION: OnceCell<Mutex<OAuthSession>> = OnceCell::new();

// Termporary constants
pub const SPOTIFY_CLIENT_ID: &str = "819a62c83de24821b2654387bc84f136";
pub const SPOTIFY_REDIRECT_URI: &str = "outify://oauth";
pub const SCOPES: &[&str] = &[
    "streaming",
    "user-read-playback-state",
    "user-modify-playback-state",
    "user-read-currently-playing",
];

use librespot_oauth::{OAuthClient, OAuthClientBuilder, OAuthToken};
use oauth2::{AuthorizationCode, PkceCodeVerifier, url::Url};

use librespot_core::Error;

pub struct OAuthSession {
    client: OAuthClient,
    pub pkce_verifier: Option<PkceCodeVerifier>,
    pub auth_url: Url,
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

        log::info!(
            "get_access_token: pkce_verifier: {}",
            pkce_verifier.secret()
        );

        let auth_code = AuthorizationCode::new(code);
        log::info!("get_access_token: auth_code: {}", auth_code.secret());

        let token_response = self
            .client
            .get_access_token_with_verifier_async(pkce_verifier, auth_code)
            .await
            .map_err(|e| Error::unavailable(format!("Unable to get OAuth token: {e}")))?;

        log::info!("get_access_token: token_response: {:#?}", token_response);
        println!("OAuth Token: {:#?}", token_response);

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
    pub async fn refresh_token(&mut self, refresh_token: String) -> Result<String, Error> {
        let refreshed = self.
            client
            .refresh_token_async(&refresh_token)
            .await
            .map_err(|e| Error::unauthenticated(format!("Unable to refresh OAuth token: {e}")))?;

        log::debug!("Refreshed OAuth token!");

        Ok(refreshed.refresh_token)
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
    let redirect = format!("{}/verify", SPOTIFY_REDIRECT_URI);

    let session = OAuthSession::new(SPOTIFY_CLIENT_ID, &redirect, SCOPES).unwrap();
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

// oAuth Get Token Pair
// Used to get the access token and refresh token
// TODO: Implement state for CSRF
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_SpAuthManager_getTokenPair(
    mut env: JNIEnv,
    _this: JObject,
    code: JString,
    _state: JString,
) -> jobjectArray {
    let code: String = env
        .get_string(&code)
        .expect("Couldn't get the JNI code!'")
        .into();

    let rt = TOKIO_RUNTIME
        .get()
        .expect("Tokio runtime is not initialized!");

    log::info!("getAccessToken: code: {}", code);
    let token = match rt.block_on(async {
        let session_mutex = OAUTH_SESSION
            .get()
            .expect("OAuth session is not initialized!");
        let mut session = session_mutex.lock().unwrap();
        log::info!(
            "getAccessToken: pkce_verifier: {}",
            session
                .pkce_verifier
                .as_ref()
                .expect("No PKCE Verifier in session")
                .secret()
        );

        session.get_access_token(code).await
    }) {
        Ok(token) => token,
        Err(e) => {
            let msg = format!("OAuth error: {e}");
            return env.new_string(msg).unwrap().into_raw();
        }
    };

    let string_class = env.find_class("java/lang/String").unwrap();
    let array = env
        .new_object_array(2, string_class, JObject::null())
        .unwrap();

    let jaccess = env.new_string(token.access_token).unwrap();
    let jrefresh = env.new_string(token.refresh_token).unwrap();

    env.set_object_array_element(&array, 0, jaccess).unwrap();
    env.set_object_array_element(&array, 1, jrefresh).unwrap();

    array.into_raw()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_SpAuthManager_refreshToken(
    mut env: JNIEnv,
    _this: JClass,
    refresh_token: JString,
) -> jstring {
    let refresh = env
        .get_string(&refresh_token)
        .expect("Couldn't get the JNI refresh token!'")
        .into();

    let rt = TOKIO_RUNTIME
        .get()
        .expect("Tokio runtime is not initialized!");

    let result = rt.block_on(async {
        let session_mutex = OAUTH_SESSION
            .get()
            .expect("OAuth session is not initialized!");
        let mut session = session_mutex.lock().unwrap();
        session.refresh_token(refresh).await
    });

    match result {
        Ok(token) => env.new_string(token).unwrap().into_raw(),
        Err(e) => {
            log::error!("Token refresh failed: {e}");
            env.new_string("").unwrap().into_raw()
        }
    }
}
