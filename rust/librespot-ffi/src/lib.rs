#[macro_use]
extern crate log;

pub mod logger;
pub use crate::logger::*; // Exporting logger macros
pub mod oauth;
mod playback;

// Exposing required librespot structs
pub use librespot_core::authentication::Credentials;
pub use librespot_playback::audio_backend::android::{AndroidSink, PcmCallback};
pub use librespot_playback::config::AudioFormat;

use once_cell::sync::OnceCell;

use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::jboolean;

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
    unsafe { std::env::set_var("RUST_BACKTRACE", "1"); }
    std::panic::set_hook(Box::new(|info| {
        log::error!("panic: {}", info);
    }));
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
