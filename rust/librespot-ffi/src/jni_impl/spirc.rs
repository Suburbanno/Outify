use jni::{
    JNIEnv,
    objects::{JClass, JString},
    sys::{jboolean, jstring},
};
use librespot_connect::{LoadContextOptions, LoadRequest, LoadRequestOptions, PlayingTrack};
use librespot_core::SpotifyUri;

use crate::{
    session::with_session,
    spirc::{SpircRuntime, with_spirc},
};

#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_spirc_Spirc_initializeSpirc(
    mut env: JNIEnv,
    _this: JClass,
) -> jboolean {
    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(rt) => rt,
        None => {
            error!("Cannot initialize spirc due to uninitialized tokio");
            return 0;
        }
    };

    let handle = rt.handle().clone();

    handle.spawn(async move {
        match crate::spirc::initialize_spirc().await {
            Ok(_) => {
                debug!("Spirc initialized!");
                notify_spirc_callback("onSpircInitialized");
            }
            Err(e) => {
                error!("Failed to initialize spirc: {e}");
            }
        }
    });

    1
}

pub fn notify_spirc_callback(static_method: &str) {
    let jvm = match crate::JVM.get() {
        Some(j) => j,
        None => {
            error!("JVM is uninitialized!");
            return;
        }
    };

    let mut env = match jvm.attach_current_thread() {
        Ok(e) => e,
        Err(e) => {
            error!("Failed to attach JNI Env: {e}");
            return;
        }
    };

    let class = env.find_class("cc/tomko/outify/core/spirc/Spirc").unwrap();
    if let Err(e) = env.call_static_method(class, static_method, "()V", &[]) {
        error!("Failed to call {static_method} callback: {e}");
    }
}

// Loads a Spotify URI specified
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_spirc_Spirc_load(
    mut env: JNIEnv,
    _this: JClass,
    juri: JString,
    jplaying_track: JString,
) -> jboolean {
    // If uri is not present - set to users liked collection
    // If uri is present - use that uri
    let uri = if juri.is_null() {
        let user_id = match with_session(|session| session.username()) {
            Ok(u) => u,
            Err(e) => {
                error!("Failed to get user_id: {e}");
                return 0;
            }
        };

        format!("spotify:user:{}:collection", user_id)
    } else {
        match env.get_string(&juri) {
            Ok(u) => u.into(),
            Err(e) => {
                warn!("Failed to get URI from JNI juri: {}", e);
                return 0;
            }
        }
    };

    let track_uri: Option<String> = if jplaying_track.is_null() {
        None
    } else {
        match env.get_string(&jplaying_track) {
            Ok(js) => Some(js.into()),
            Err(e) => {
                error!("failed to get playing track uri: {}", e);
                return 0;
            }
        }
    };

    let track = match track_uri {
        Some(uri) => Some(PlayingTrack::Uri(uri)),
        None => None,
    };

    let options = LoadRequestOptions {
        start_playing: true,
        playing_track: track,
        ..Default::default()
    };

    let req = LoadRequest::from_context_uri(uri, options);

    match with_spirc(|runtime| runtime.load(req)) {
        Ok(Ok(_)) => 1 as jboolean,
        Ok(Err(e)) => {
            error!("Failed to load Spirc: {}", e);
            0 as jboolean
        }
        Err(e) => {
            error!("Spirc not available: {}", e);
            0 as jboolean
        }
    }
}

// Adds a Spotify URI to queue
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_spirc_Spirc_addToQueue(
    mut env: JNIEnv,
    _this: JClass,
    juri: JString,
) -> jboolean {
    let uri: String = match env.get_string(&juri) {
        Ok(u) => u.into(),
        Err(e) => {
            warn!("Failed to get URI from JNI juri: {}", e);
            return 0;
        }
    };

    let spotify_uri = match SpotifyUri::from_uri(uri.as_str()) {
        Ok(uri) => uri,
        Err(e) => {
            warn!("failed to get SpotifyURI: {}", e);
            return 0;
        }
    };

    match with_spirc(|runtime| runtime.add_to_queue(spotify_uri)) {
        Ok(Ok(_)) => 1,
        Ok(Err(e)) => {
            warn!("Failed to add to queue: {}", e);
            0
        }
        Err(e) => {
            warn!("Failed to add to queue: {}", e);
            0
        }
    }
}

// Activates the Spirc session
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_spirc_Spirc_activate(
    _env: JNIEnv,
    _this: JClass,
) -> jboolean {
    match with_spirc(|runtime| runtime.activate()) {
        Ok(Ok(_)) => 1,
        Ok(Err(e)) => {
            warn!("Failed to activate spirc: {}", e);
            0
        }
        Err(e) => {
            warn!("Failed to activate spirc: {}", e);
            0
        }
    }
}

// Transfers the Spirc session to us
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_spirc_Spirc_transfer(
    _env: JNIEnv,
    _this: JClass,
) -> jboolean {
    match with_spirc(|runtime| runtime.transfer()) {
        Ok(Ok(_)) => 1,
        Ok(Err(e)) => {
            warn!("Failed to transfer spirc: {}", e);
            0
        }
        Err(e) => {
            warn!("Failed to transfer spirc: {}", e);
            0
        }
    }
}

// Plays the player
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_spirc_Spirc_playerPlay(
    _env: JNIEnv,
    _this: JClass,
) -> jboolean {
    match with_spirc(|runtime| runtime.play()) {
        Ok(Ok(_)) => 1,
        Ok(Err(e)) => {
            warn!("Failed to play spirc: {}", e);
            0
        }
        Err(e) => {
            warn!("Failed to play spirc: {}", e);
            0
        }
    }
}

// Pauses the player
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_spirc_Spirc_playerPause(
    _env: JNIEnv,
    _this: JClass,
) -> jboolean {
    match with_spirc(|runtime| runtime.pause()) {
        Ok(Ok(_)) => 1,
        Ok(Err(e)) => {
            warn!("Failed to pause spirc: {}", e);
            0
        }
        Err(e) => {
            warn!("Failed to pause spirc: {}", e);
            0
        }
    }
}

// Plays/Pauses the player
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_spirc_Spirc_playerPlayPause(
    _env: JNIEnv,
    _this: JClass,
) -> jboolean {
    match with_spirc(|runtime| runtime.play_pause()) {
        Ok(Ok(_)) => 1,
        Ok(Err(e)) => {
            warn!("Failed to play_pause spirc: {}", e);
            0
        }
        Err(e) => {
            warn!("Failed to play_pause spirc: {}", e);
            0
        }
    }
}

// Plays the next track
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_spirc_Spirc_playerNext(
    _env: JNIEnv,
    _this: JClass,
) -> jboolean {
    match with_spirc(|runtime| runtime.next()) {
        Ok(Ok(_)) => 1,
        Ok(Err(e)) => {
            warn!("Failed to player next spirc: {}", e);
            0
        }
        Err(e) => {
            warn!("Failed to player next spirc: {}", e);
            0
        }
    }
}

// Plays the previous track
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_spirc_Spirc_playerPrevious(
    _env: JNIEnv,
    _this: JClass,
) -> jboolean {
    match with_spirc(|runtime| runtime.prev()) {
        Ok(Ok(_)) => 1,
        Ok(Err(e)) => {
            warn!("Failed to player prev spirc: {}", e);
            0
        }
        Err(e) => {
            warn!("Failed to player prev spirc: {}", e);
            0
        }
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_spirc_Spirc_previousTracks(
    env: JNIEnv,
    _this: JClass,
) -> jstring {
    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            error!("failed to get Tokio runtime!");
            return std::ptr::null_mut();
        }
    };

    let tracks_raw =
        match with_spirc(|runtime| rt.block_on(async move { runtime.prev_tracks().await })) {
            Ok(Ok(tracks)) => tracks, // success: outer Ok, inner Ok
            Ok(Err(e)) => {
                error!("failed to fetch tracks: {}", e);
                return std::ptr::null_mut();
            }
            Err(e) => {
                error!("spirc not available: {}", e);
                return std::ptr::null_mut();
            }
        };
    let uris: Vec<String> = tracks_raw.into_iter().map(|track| track.uri).collect();

    let json = match serde_json::to_string(&uris) {
        Ok(j) => j,
        Err(e) => {
            error!("serde_json: {}", e);
            "[]".to_string()
        }
    };

    match env.new_string(&json) {
        Ok(jni_str) => jni_str.into_raw(),
        Err(e) => {
            error!("failed to convert json into json: {}", e);
            std::ptr::null_mut()
        }
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_spirc_Spirc_nextTracks(
    env: JNIEnv,
    _this: JClass,
) -> jstring {
    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            error!("failed to get Tokio runtime!");
            return std::ptr::null_mut();
        }
    };

    let tracks_raw =
        match with_spirc(|runtime| rt.block_on(async move { runtime.next_tracks().await })) {
            Ok(Ok(tracks)) => tracks,
            Ok(Err(e)) => {
                error!("failed to fetch tracks: {}", e);
                return std::ptr::null_mut();
            }
            Err(e) => {
                error!("spirc not available: {}", e);
                return std::ptr::null_mut();
            }
        };

    let uris: Vec<String> = tracks_raw.into_iter().map(|track| track.uri).collect();

    let json = match serde_json::to_string(&uris) {
        Ok(j) => j,
        Err(e) => {
            error!("serde_json: {}", e);
            "[]".to_string()
        }
    };

    match env.new_string(&json) {
        Ok(jni_str) => jni_str.into_raw(),
        Err(e) => {
            error!("failed to convert json into json: {}", e);
            std::ptr::null_mut()
        }
    }
}
