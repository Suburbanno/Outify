use std::sync::Mutex;

use jni::{
    JNIEnv,
    objects::{GlobalRef, JClass, JObject, JValue},
};
use librespot_metadata::Track;
use once_cell::sync::OnceCell;

pub static PLAYER_EVENT_LISTENER: OnceCell<Mutex<GlobalRef>> = OnceCell::new();

// Registers the track update callback and stores its GlobalRef
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_playback_PlaybackManager_registerPlayerEventListener(
    env: JNIEnv,
    _this: JClass,
    callback: JObject,
) {
    let jvm = crate::JVM.get().unwrap();

    let global = env.new_global_ref(callback).unwrap();
    PLAYER_EVENT_LISTENER.set(Mutex::new(global)).ok();
}
