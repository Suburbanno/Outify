#[macro_use]
extern crate log;

pub mod jni_impl;
pub mod jni_utils;
pub mod metadata;

pub mod oauth;
pub mod session;
pub mod spclient;
pub mod spotify;
pub mod outifyuri;

mod playback;
mod profile;
mod spirc;

use std::path::PathBuf;
use std::sync::atomic::AtomicBool;

// Exposing required librespot structs
pub use librespot_core::authentication::Credentials;
pub use librespot_playback::audio_backend::android::{AndroidSink, PcmCallback};
pub use librespot_playback::config::AudioFormat;

use once_cell::sync::OnceCell;

use jni::JNIEnv;
use jni::JavaVM;
use jni::objects::{GlobalRef, JClass, JObject};
use jni::sys::jint;

use tokio::runtime::Runtime;

static TOKIO_RUNTIME: OnceCell<Runtime> = OnceCell::new();
static JVM: OnceCell<JavaVM> = OnceCell::new();

static FILES_DIR: OnceCell<PathBuf> = OnceCell::new();
static CACHE_DIR: OnceCell<PathBuf> = OnceCell::new();

#[unsafe(no_mangle)]
pub extern "system" fn JNI_OnLoad(vm: JavaVM, _reserved: *mut std::os::raw::c_void) -> jint {
    let _ = JVM.set(vm);
    jni::sys::JNI_VERSION_1_6
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_LibrespotFfi_libInit(
    mut env: JNIEnv,
    _class: JClass,
    context: JObject,
) {
    let jvm = env.get_java_vm().unwrap();

    // Setup TOKIO
    TOKIO_RUNTIME.get_or_init(|| {
        tokio::runtime::Builder::new_multi_thread()
            .enable_all()
            .build()
            .expect("Failed to create Tokio runtime!")
    });

    // Initialize logger
    crate::jni_utils::logger::AndroidLogger::init(jvm, log::LevelFilter::Debug).unwrap();
    unsafe {
        std::env::set_var("RUST_BACKTRACE", "1");
    }
    std::panic::set_hook(Box::new(|info| {
        log::error!("panic: {}", info);
    }));

    // Setting up directory
    let files_dir = crate::jni_utils::folders::get_android_dir(&mut env, &context, "getFilesDir");
    let cache_dir = crate::jni_utils::folders::get_android_dir(&mut env, &context, "getCacheDir");

    if FILES_DIR.set(files_dir).is_err() {
        error!("Failed to set files dir concurrently!");
    }
    if CACHE_DIR.set(cache_dir).is_err() {
        error!("Failed to set cache dir concurrently!");
    }

    session::init_session_container();
    spirc::init_spirc_container();

    spotify::client::init_client();
}
