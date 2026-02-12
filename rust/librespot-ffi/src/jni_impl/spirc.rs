use jni::{
    objects::{JClass, JString}, sys::{jboolean, jstring}, JNIEnv
};
use librespot_connect::LoadRequest;
use librespot_core::SpotifyUri;

use crate::spirc::SpircRuntime;

// Initializes the [SpircRuntime] into OnceCell.
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_spirc_Spirc_initializeSpirc(
    env: JNIEnv,
    this: JClass,
) -> jboolean {
    debug!("Initializing SpircRuntime");

    if !super::SPIRC_RUNTIME.get().is_none() {
        warn!("SpircRuntime already initialized!");
        return 0 as jboolean;
    }

    let session = match crate::session::SESSION.get() {
        Some(s) => s,
        None => {
            error!("Cannot initialize SpircRuntime as SESSION isn't initialized!");
            return 0 as jboolean;
        }
    };

    if session.cache().is_none() {
        error!("Cannot initialize SpircRuntime as session cache is none!");
        return 0 as jboolean;
    }

    let cache = session.cache().unwrap();
    let credentials = match cache.credentials() {
        Some(c) => c,
        None => {
            error!("Cannot initialize SpircRuntime as cached credentials are None!");
            return 0 as jboolean;
        }
    };

    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            error!("TOKIO_RUNTIME isn't initialized in SpircRuntime (jni_impl)!'");
            return 0 as jboolean;
        }
    };

    let global_this = match env.new_global_ref(this) {
        Ok(r) => r,
        Err(e) => {
            error!("Failed to create GlobalRef: {}", e);
            return 0 as jboolean;
        }
    };

    let jvm = crate::JVM.get().expect("JVM not initialized!").clone();
    rt.spawn(async move {
        let runtime = match SpircRuntime::new(&session, credentials).await {
            Ok(r) => r,
            Err(e) => {
                error!("Failed to create SpircRuntime with err: {}", e);
                return;
            }
        };

        if super::SPIRC_RUNTIME.set(runtime).is_err() {
            warn!("Cannot set SPIRC_RUNTIME concurrently!");
            return;
        }

        debug!("SpircRuntime initialized");

        let mut env = match jvm.attach_current_thread() {
            Ok(e) => e,
            Err(e) => {
                error!("Failed to attach JVM to thread: {}", e);
                return;
            }
        };

        // Calling callback
        if let Err(e) = env.call_method(global_this, "onSpircInitialized", "()V", &[]) {
            error!("Failed to call onSpircInitialized callback: {}", e);
        }
    });

    1 as jboolean
}

// Loads a Spotify URI specified
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_spirc_Spirc_load(
    mut env: JNIEnv,
    _this: JClass,
    juri: JString,
) -> jboolean {
    let runtime = match super::SPIRC_RUNTIME.get() {
        Some(r) => r,
        None => {
            warn!("Spirc not initialized for load");
            return 0;
        }
    };

    let uri = match env.get_string(&juri) {
        Ok(u) => u.into(),
        Err(e) => {
            warn!("Failed to get URI from JNI juri: {}", e);
            return 0;
        }
    };

    let req = LoadRequest::from_context_uri(
        uri,
        librespot_connect::LoadRequestOptions {
            ..Default::default()
        },
    );

    match runtime.load(req) {
        Ok(_) => {}
        Err(e) => {
            warn!("Failed to Spirc load: {}", e);
        }
    }

    1
}

// Adds a Spotify URI to queue
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_spirc_Spirc_addToQueue(
    mut env: JNIEnv,
    _this: JClass,
    juri: JString,
) -> jboolean {
    let runtime = match super::SPIRC_RUNTIME.get() {
        Some(r) => r,
        None => {
            warn!("Spirc not initialized for load");
            return 0;
        }
    };

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

    match runtime.add_to_queue(spotify_uri) {
        Ok(_) => {
            return 1
        },
        Err(e) => {
            warn!("Failed to add to queue: {}", e);
            return 0
        }
    }
}

// Activates the Spirc session
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_spirc_Spirc_activate(
    _env: JNIEnv,
    _this: JClass,
) -> jboolean {
    let runtime = match super::SPIRC_RUNTIME.get() {
        Some(r) => r,
        None => {
            warn!("Spirc not initialized for activation");
            return 0;
        }
    };

    match runtime.activate() {
        Ok(_) => {}
        Err(e) => {
            error!("Failed to activate Spirc: {}", e);
            return 0 as jboolean;
        }
    }

    1 as jboolean
}

// Transfers the Spirc session to us
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_spirc_Spirc_transfer(
    _env: JNIEnv,
    _this: JClass,
) -> jboolean {
    let runtime = match super::SPIRC_RUNTIME.get() {
        Some(r) => r,
        None => {
            warn!("Spirc not initialized for transfer");
            return 0;
        }
    };

    match runtime.transfer() {
        Ok(_) => {}
        Err(e) => {
            error!("Failed to activate Spirc: {}", e);
            return 0 as jboolean;
        }
    }

    1 as jboolean
}

// Plays the player
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_spirc_Spirc_playerPlay(
    _env: JNIEnv,
    _this: JClass,
) -> jboolean {
    let runtime = match super::SPIRC_RUNTIME.get() {
        Some(r) => r,
        None => {
            warn!("Spirc not initialized for transfer");
            return 0;
        }
    };

    match runtime.play() {
        Ok(_) => {}
        Err(e) => {
            error!("Failed to play Spirc: {}", e);
            return 0 as jboolean;
        }
    }

    1 as jboolean
}

// Pauses the player
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_spirc_Spirc_playerPause(
    _env: JNIEnv,
    _this: JClass,
) -> jboolean {
    let runtime = match super::SPIRC_RUNTIME.get() {
        Some(r) => r,
        None => {
            warn!("Spirc not initialized for transfer");
            return 0;
        }
    };

    match runtime.pause() {
        Ok(_) => {}
        Err(e) => {
            error!("Failed to pause Spirc: {}", e);
            return 0 as jboolean;
        }
    }

    1 as jboolean
}

// Plays/Pauses the player
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_spirc_Spirc_playerPlayPause(
    _env: JNIEnv,
    _this: JClass,
) -> jboolean {
    let runtime = match super::SPIRC_RUNTIME.get() {
        Some(r) => r,
        None => {
            warn!("Spirc not initialized for transfer");
            return 0;
        }
    };

    match runtime.play_pause() {
        Ok(_) => {}
        Err(e) => {
            error!("Failed to play_pause Spirc: {}", e);
            return 0 as jboolean;
        }
    }

    1 as jboolean
}

// Plays the next track
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_spirc_Spirc_playerNext(
    _env: JNIEnv,
    _this: JClass,
) -> jboolean {
    let runtime = match super::SPIRC_RUNTIME.get() {
        Some(r) => r,
        None => {
            warn!("Spirc not initialized for transfer");
            return 0;
        }
    };

    match runtime.next() {
        Ok(_) => {}
        Err(e) => {
            error!("Failed to next Spirc: {}", e);
            return 0 as jboolean;
        }
    }

    1 as jboolean
}

// Plays the previous track
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_spirc_Spirc_playerPrevious(
    _env: JNIEnv,
    _this: JClass,
) -> jboolean {
    let runtime = match super::SPIRC_RUNTIME.get() {
        Some(r) => r,
        None => {
            warn!("Spirc not initialized for transfer");
            return 0;
        }
    };

    match runtime.prev() {
        Ok(_) => {}
        Err(e) => {
            error!("Failed to prev Spirc: {}", e);
            return 0 as jboolean;
        }
    }

    1 as jboolean
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_spirc_Spirc_previousTracks(
    env: JNIEnv,
    _this: JClass,
) -> jstring {
    let runtime = match super::SPIRC_RUNTIME.get() {
        Some(r) => r,
        None => {
            warn!("Spirc not initialized for previousTracks");
            return std::ptr::null_mut();
        }
    };

    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            error!("failed to get Tokio runtime!");
            return std::ptr::null_mut();
        }
    };

    let tracks_raw: Vec<librespot_protocol::player::ProvidedTrack> = match rt.block_on(async move {
        runtime.prev_tracks().await
    }) {
        Ok(tracks) => tracks,
        Err(e) => {
            error!("failed to fetch tracks: {}", e);
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
    let runtime = match super::SPIRC_RUNTIME.get() {
        Some(r) => r,
        None => {
            warn!("Spirc not initialized for previousTracks");
            return std::ptr::null_mut();
        }
    };

    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            error!("failed to get Tokio runtime!");
            return std::ptr::null_mut();
        }
    };

    let tracks_raw: Vec<librespot_protocol::player::ProvidedTrack> = match rt.block_on(async move {
        runtime.next_tracks().await
    }) {
        Ok(tracks) => tracks,
        Err(e) => {
            error!("failed to fetch tracks: {}", e);
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
