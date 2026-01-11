use librespot_api as api;
use api::Error;

use jni::{objects::{JClass, JObject}, sys::{jboolean, jstring}, JNIEnv};

// LibrespotFfi isConnected
// Used for checking, whether the Rust <> JNI connection works
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_LibrespotFfi_isConnected(_env: JNIEnv, _class: JClass) -> jboolean {
    1
}

/// oAuth Get Auth URL
/// Used for opening the Spotify authorization in browser
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_SpAuthManager_getAuthURL(env: JNIEnv, _this: JObject) -> jstring {
    let rt = tokio::runtime::Runtime::new().unwrap();
    let url = rt.block_on(api::oauth_get_auth_url()).unwrap();

    let output = env.new_string(url).unwrap();
    output.into_raw()
}

// oAuth Get Access Token
// Used to get the access token
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_SpAuthManager_oauthGetAccessToken(env: JNIEnv, _this: JObject) -> jstring {
    let rt = tokio::runtime::Runtime::new().unwrap();
    let token = rt.block_on(api::oauth_get_access_token()).unwrap();

    let output = env.new_string(token.access_token).unwrap();
    output.into_raw()
}

