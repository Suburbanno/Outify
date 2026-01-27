use jni::{
    JNIEnv,
    objects::{GlobalRef, JClass, JMethodID, JObject, JValue},
    sys::jboolean,
};
use librespot_metadata::Track;

// Converts native track into kotlin compatible NativeTrack
pub fn convert_track(track: &Track) -> Option<JObject> {
    let jvm = match crate::JVM.get() {
        Some(j) => j,
        None => {
            warn!("Cannot get class NativeTrack because JVM is not set!",);
            return None;
        }
    };

    let mut env = match jvm.get_env() {
        Ok(e) => e,
        Err(e) => {
            warn!("failed to get env: {}", e);
            return None;
        }
    };

    let class = env
        .find_class("cc/tomko/outify/data/native/NativeTrack")
        .expect("NativeTrack class not found");

    let id = env.new_string(track.id.to_uri()).unwrap();
    let name = env.new_string(track.name.clone()).unwrap();

    Some(
        env.new_object(
            class,
            "(Ljava/lang/String;Ljava/lang/String;IIZ)V",
            &[
                JValue::Object(&id),
                JValue::Object(&name),
                JValue::Int(track.duration),
                JValue::Int(track.popularity),
                JValue::Bool(track.is_explicit as jboolean),
            ],
        )
        .expect("Failed to create NativeTrack"),
    )
}
