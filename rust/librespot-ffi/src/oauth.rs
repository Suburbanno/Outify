use librespot_api as api;

use crate::{logd, loge, logi, logv}; // Exporting logger macros

use api::oauth::OAuthSession;
use jni::{
    JNIEnv,
    objects::{JClass, JObject, JString},
    sys::{jboolean, jobjectArray, jstring},
};
use once_cell::sync::OnceCell;
use std::sync::Mutex;

static TOKIO_RT: OnceCell<tokio::runtime::Runtime> = OnceCell::new();
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

/// Initializes the OAuth Session.
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_SpAuthManager_initialize(
    mut env: JNIEnv,
    _this: JObject,
    client_id: jstring,
    redirect_uri: jstring,
    scopes: jstring,
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
    let rt = TOKIO_RT.get_or_init(|| tokio::runtime::Runtime::new().unwrap());

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
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_SpAuthManager_getTokenPair(
    mut env: JNIEnv,
    _this: JObject,
    code: JString,
    state: JString,
) -> jobjectArray {
    let code: String = env
        .get_string(&code)
        .expect("Couldn't get the JNI code!'")
        .into();

    log::info!("getAccessToken: code: {}", code);
    let rt = TOKIO_RT.get_or_init(|| tokio::runtime::Runtime::new().unwrap());
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
