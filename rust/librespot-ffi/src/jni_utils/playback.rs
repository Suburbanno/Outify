use jni::{
    objects::{GlobalRef, JObject, JValue},
    sys::jboolean,
};
use librespot_core::SpotifyUri;
use librespot_metadata::Metadata;
use librespot_playback::player::PlayerEvent;

use crate::session::with_session;

// TODO: Optimize thread attachments, JNI calls in total

// Updates the Outify track
pub fn on_player_track_update(track_id: SpotifyUri) {
    let jvm = match crate::JVM.get() {
        Some(j) => j,
        None => {
            error!("failed to propagate player paused event: jvm not initialized");
            return;
        }
    };

    let listener_ref: GlobalRef = match crate::jni_impl::playback::PLAYER_EVENT_LISTENER.get() {
        Some(cell) => {
            let guard = cell.lock().unwrap();
            guard.clone()
        }
        None => {
            error!("failed to propagate player paused event due to listener object not set");
            return;
        }
    };

    tokio::spawn({
        let session = match with_session(|s| s.clone()) {
            Ok(s) => s,
            Err(e) => {
                error!("Session unavailable: {}", e);
                return;
            }
        };
        async move {
            let maybe_metadata = match librespot_metadata::Track::get(&session, &track_id).await {
                Ok(m) => Some(m),
                Err(e) => {
                    error!("failed to fetch metadata: {}", e);
                    None
                }
            };

            let json = match maybe_metadata {
                Some(metadata) => {
                    let track = crate::metadata::track::TrackJson::from(&metadata);
                    match serde_json::to_string(&track) {
                        Ok(s) => s,
                        Err(e) => {
                            error!("serde_json serialization failed: {}", e);
                            return;
                        }
                    }
                }
                None => {
                    // TODO: call java with no JSON?
                    return;
                }
            };

            let mut env = match jvm.attach_current_thread() {
                Ok(e) => e,
                Err(e) => {
                    error!("failed to attach thread to JVM: {}", e);
                    return;
                }
            };

            let j_uri = match env.new_string(&track_id.to_uri()) {
                Ok(s) => s,
                Err(e) => {
                    error!("failed to create jstring for uri: {}", e);
                    return;
                }
            };
            let j_json = match env.new_string(&json) {
                Ok(s) => s,
                Err(e) => {
                    error!("failed to create jstring for json: {}", e);
                    return;
                }
            };

            if let Err(e) = env.call_method(
                listener_ref.as_obj(),
                "onTrackChange",
                "(Ljava/lang/String;Ljava/lang/String;)V",
                &[JValue::Object(&j_uri), JValue::Object(&j_json)],
            ) {
                error!("failed to call onTrackChange callback: {:?}", e);
            }

            if let Ok(true) = env.exception_check() {
                env.exception_describe().ok();
                env.exception_clear().ok();
            }
        }
    });
}

// Updates the Outify player position
pub fn on_player_position_update(position_ms: u32, track_id: SpotifyUri) {
    let jvm = match crate::JVM.get() {
        Some(j) => j,
        None => {
            error!("failed to propagate player paused event: jvm not initialized");
            return;
        }
    };

    let listener_ref: GlobalRef = match crate::jni_impl::playback::PLAYER_EVENT_LISTENER.get() {
        Some(cell) => {
            let guard = cell.lock().unwrap();
            guard.clone()
        }
        None => {
            error!("failed to propagate player paused event due to listener object not set");
            return;
        }
    };

    tokio::spawn({
        let session = match with_session(|s| s.clone()) {
            Ok(s) => s,
            Err(e) => {
                error!("Session unavailable: {}", e);
                return;
            }
        };

        async move {
            let maybe_metadata = match librespot_metadata::Track::get(&session, &track_id).await {
                Ok(m) => Some(m),
                Err(e) => {
                    error!("failed to fetch metadata: {}", e);
                    None
                }
            };

            let json = match maybe_metadata {
                Some(metadata) => {
                    let track = crate::metadata::track::TrackJson::from(&metadata);
                    match serde_json::to_string(&track) {
                        Ok(s) => s,
                        Err(e) => {
                            error!("serde_json serialization failed: {}", e);
                            return;
                        }
                    }
                }
                None => {
                    // TODO: call java with no JSON?
                    return;
                }
            };

            let mut env = match jvm.attach_current_thread() {
                Ok(e) => e,
                Err(e) => {
                    error!("failed to attach thread to JVM: {}", e);
                    return;
                }
            };

            let j_uri = match env.new_string(&track_id.to_uri()) {
                Ok(s) => s,
                Err(e) => {
                    error!("failed to create jstring for uri: {}", e);
                    return;
                }
            };
            let j_json = match env.new_string(&json) {
                Ok(s) => s,
                Err(e) => {
                    error!("failed to create jstring for json: {}", e);
                    return;
                }
            };

            if let Err(e) = env.call_method(
                listener_ref.as_obj(),
                "onPositionUpdate",
                "(Ljava/lang/String;JLjava/lang/String;)V",
                &[
                    JValue::Object(&j_uri),
                    JValue::Long(position_ms as i64),
                    JValue::Object(&j_json),
                ],
            ) {
                error!("failed to call onPositionUpdate callback: {:?}", e);
            }

            if let Ok(true) = env.exception_check() {
                env.exception_describe().ok();
                env.exception_clear().ok();
            }
        }
    });
}

// Updates the Outify playing status
pub fn on_player_status(playing: bool) {
    let jvm = match crate::JVM.get() {
        Some(j) => j,
        None => {
            error!("failed to propagate player paused event: jvm not initialized");
            return;
        }
    };

    let listener_ref: GlobalRef = match crate::jni_impl::playback::PLAYER_EVENT_LISTENER.get() {
        Some(cell) => {
            let guard = cell.lock().unwrap();
            guard.clone()
        }
        None => {
            error!("failed to propagate player paused event due to listener object not set");
            return;
        }
    };

    let session = match crate::session::SESSION.get() {
        Some(s) => s.clone(),
        None => {
            error!("failed to retrieve session: not initialized");
            return;
        }
    };

    tokio::spawn(async move {
        let mut env = match jvm.attach_current_thread() {
            Ok(e) => e,
            Err(e) => {
                error!("failed to attach thread to JVM: {}", e);
                return;
            }
        };

        if let Err(e) = env.call_method(
            listener_ref.as_obj(),
            "onPlayingStatus",
            "(Z)V",
            &[JValue::Bool(playing as jboolean)],
        ) {
            error!("failed to call onPlayerStatus callback: {:?}", e);
        }

        if let Ok(true) = env.exception_check() {
            env.exception_describe().ok();
            env.exception_clear().ok();
        }
    });
}
