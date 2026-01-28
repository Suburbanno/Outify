use jni::objects::{GlobalRef, JObject, JValue};
use librespot_core::SpotifyUri;
use librespot_metadata::Metadata;
use librespot_playback::player::PlayerEvent;

// Calls the UI callback whenever [PlayerEvent] is received by Spirc
pub fn on_player_playing(event: PlayerEvent) {
    let jvm = match crate::JVM.get() {
        Some(j) => j,
        None => {
            error!("failed to propagate player playing event: jvm not initialized");
            return;
        }
    };

    let mut env = match jvm.attach_current_thread() {
        Ok(e) => e,
        Err(e) => {
            error!("failed to propagate player playing event to the ui: {}", e);
            return;
        }
    };

    if let PlayerEvent::Playing {
        play_request_id,
        track_id,
        position_ms,
    } = event
    {
        let listener_ref: GlobalRef = match crate::jni_impl::playback::PLAYER_EVENT_LISTENER.get() {
            Some(cell) => {
                let guard = cell.lock().unwrap();
                guard.clone()
            }
            None => {
                error!("failed to propagate player playing event due to listener object not set");
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

        let pos = position_ms as i64;
        let req_id = play_request_id as i64;

        tokio::spawn(async move {
            let maybe_metadata = match librespot_metadata::Track::get(&session, &track_id).await {
                Ok(m) => Some(m),
                Err(e) => {
                    error!("failed to fetch metadata: {}", e);
                    None
                }
            };

            let json = match maybe_metadata {
                Some(metadata) => {
                    let track = crate::jni_utils::native_metadata::TrackJson::from(&metadata);
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
                "onPlaying",
                "(Ljava/lang/String;JJLjava/lang/String;)V",
                &[
                    JValue::Object(&j_uri),
                    JValue::Long(pos),
                    JValue::Long(req_id),
                    JValue::Object(&j_json),
                ],
            ) {
                error!("failed to call onPlayingWithMeta callback: {:?}", e);
            }

            if let Ok(true) = env.exception_check() {
                env.exception_describe().ok();
                env.exception_clear().ok();
            }
        });
    }
}
