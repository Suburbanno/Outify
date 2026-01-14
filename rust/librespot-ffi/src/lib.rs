pub mod logger;
pub use crate::logger::*; // Exporting logger macros
pub mod oauth;
mod playback;

use librespot_api as api;

use api::{AndroidSink, AudioFormat, Error, PcmCallback, oauth::OAuthSession};

use std::cell::RefCell;
use std::ffi::c_void;
use std::mem;
use std::sync::Mutex;

use once_cell::sync::OnceCell;

use jni::objects::{AutoLocal, JClass, JObject, JString, JThrowable, JValue};
use jni::sys::{jboolean, jlong, jstring};
use jni::{JNIEnv, JNIVersion, JavaVM, sys};

use tokio::runtime::Runtime;

static TOKIO_RUNTIME: OnceCell<Runtime> = OnceCell::new();

#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_LibrespotFfi_libInit(env: JNIEnv, _class: JClass) {
    let jvm = env.get_java_vm().unwrap();
    TOKIO_RUNTIME.get_or_init(|| {
        tokio::runtime::Builder::new_multi_thread()
            .enable_all()
            .build()
            .expect("Failed to create Tokio runtime!")
    });

    AndroidLogger::init(jvm, log::LevelFilter::Debug).unwrap();
}

// LibrespotFfi isConnected
// Used for checking, whether the Rust <> JNI connection works
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_LibrespotFfi_isConnected(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    1
}
