pub mod oauth;

use librespot_api as api;
use api::{Error,oauth::OAuthSession};

use jni::{objects::{JClass, JObject}, sys::{jboolean, jstring}, JNIEnv};

// LibrespotFfi isConnected
// Used for checking, whether the Rust <> JNI connection works
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_LibrespotFfi_isConnected(_env: JNIEnv, _class: JClass) -> jboolean {
    1
}

