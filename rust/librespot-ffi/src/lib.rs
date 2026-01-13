pub mod logger;
pub use crate::logger::*; // Exporting logger macros
pub mod oauth;

use librespot_api as api;

use api::{Error, oauth::OAuthSession};

use std::cell::RefCell;
use std::sync::Mutex;

use once_cell::sync::OnceCell;

use jni::objects::{AutoLocal, JClass, JObject, JString, JThrowable, JValue};
use jni::sys::{jboolean, jstring};
use jni::{JNIEnv, JNIVersion, JavaVM, sys};

use tokio::runtime::Runtime;

// LibrespotFfi isConnected
// Used for checking, whether the Rust <> JNI connection works
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_LibrespotFfi_isConnected(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    1
}
