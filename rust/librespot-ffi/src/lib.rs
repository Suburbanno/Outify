#[macro_use]
extern crate log;

pub mod logger;
pub use crate::logger::*; // Exporting logger macros
pub mod oauth;
pub mod session;
mod playback;

// Exposing required librespot structs
use librespot_core::Session;
pub use librespot_core::authentication::Credentials;
pub use librespot_playback::audio_backend::android::{AndroidSink, PcmCallback};
pub use librespot_playback::config::AudioFormat;

use once_cell::sync::OnceCell;

use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::jboolean;

use tokio::runtime::Runtime;

static TOKIO_RUNTIME: OnceCell<Runtime> = OnceCell::new();

// Constants
pub use librespot_core::config::ANDROID_CLIENT_ID;
pub const OUTIFY_CLIENT_ID: &str = "819a62c83de24821b2654387bc84f136"; // Outifys client_id, used for OAuth
pub const SPOTIFY_CALLBACK_URI: &str = "outify://oauth";
pub const SCOPES: &[&str] = &[
    "streaming",
    "user-read-playback-state",
    "user-modify-playback-state",
    "user-read-currently-playing",
];


#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_LibrespotFfi_libInit(env: JNIEnv, _class: JClass) {
    let jvm = env.get_java_vm().unwrap();

    // Setup TOKIO
    TOKIO_RUNTIME.get_or_init(|| {
        tokio::runtime::Builder::new_multi_thread()
            .enable_all()
            .build()
            .expect("Failed to create Tokio runtime!")
    });

    // Initialize logger
    AndroidLogger::init(jvm, log::LevelFilter::Debug).unwrap();
    unsafe {
        std::env::set_var("RUST_BACKTRACE", "1");
    }
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
