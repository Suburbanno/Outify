use jni::{
    JNIEnv,
    objects::{JClass, JObject, JObjectArray, JString},
    sys::{jboolean, jint, jobjectArray, jstring},
};
use librespot_core::{SpotifyId, SpotifyUri};
use librespot_metadata::Metadata;
use regex::Regex;

use crate::{
    jni_utils::vec_to_jstring_array, outifyuri::OutifyUri, session::with_session,
    spotify::client::get_client,
};

#[unsafe(export_name = "Java_cc_tomko_outify_core_SpClient_username")]
pub extern "system" fn username(env: JNIEnv, _class: JClass) -> jstring {
    let username = match crate::spclient::get_username() {
        Ok(u) => u,
        Err(e) => {
            error!("failed to get username: {e}");
            return std::ptr::null_mut();
        }
    };

    match env.new_string(username) {
        Ok(u) => u.into_raw(),
        Err(e) => {
            error!("Failed to convert JString: {e}");
            return std::ptr::null_mut();
        }
    }
}

#[unsafe(export_name = "Java_cc_tomko_outify_core_SpClient_getCurrentUserProfile")]
pub extern "system" fn get_current_user(env: JNIEnv, _class: JClass) -> jstring {
    let client = get_client();
    
    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            error!("failed to get Tokio runtime!");
            return std::ptr::null_mut();
        }
    };

    let result = match rt.block_on(async { client.get_current_user().await }) {
        Ok(r) => {
            match serde_json::to_string(&r) {
                Ok(j) => j,
                Err(e) => {
                    error!("failed to convert struct to json: {e}");
                    return std::ptr::null_mut();
                },
            }
        },
        Err(e) => {
            error!("failed to get current user: {e}");
            return std::ptr::null_mut();
        },
    };

    match env.new_string(&result) {
        Ok(s) => s.into_raw(),
        Err(e) => {
            error!("failed to convert JString: {e}");
            return std::ptr::null_mut();
        },
    }
}


#[unsafe(export_name = "Java_cc_tomko_outify_core_SpClient_search")]
pub extern "system" fn spotify_search(
    mut env: JNIEnv,
    _class: JClass,
    query: JString,
    jtype: JString,
    offset: jint,
    limit: jint,
) -> jobjectArray {
    let client = get_client();

    let query: String = match env.get_string(&query) {
        Ok(q) => q.into(),
        Err(e) => {
            error!("failed to get query as string: {}", e);
            return std::ptr::null_mut();
        }
    };

    let jtype: String = match env.get_string(&jtype) {
        Ok(q) => q.into(),
        Err(e) => {
            error!("failed to get jtype as string: {}", e);
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

    let limit = if limit == -1 { None } else { Some(limit) };
    let offset = if offset == -1 { None } else { Some(offset) };

    let uris_res = rt.block_on(async { client.search(&query, &jtype, limit, offset).await });

    let uris = match uris_res {
        Ok(u) => u,
        Err(e) => {
            error!("failed to search spotify: {e}");
            return std::ptr::null_mut();
        }
    };

    vec_to_jstring_array(&mut env, uris)
}

#[unsafe(export_name = "Java_cc_tomko_outify_core_SpClient_saveItems")]
pub extern "system" fn save_item(mut env: JNIEnv, _class: JClass, uris: JObjectArray) -> jboolean {
    let client = get_client();

    let length = env.get_array_length(&uris).unwrap();
    let mut rust_uris = Vec::with_capacity(length as usize);

    for i in 0..length {
        let obj = env.get_object_array_element(&uris, i).unwrap();
        let jstr: JString = JString::from(obj);
        let rust_string: String = env.get_string(&jstr).unwrap().into();
        rust_uris.push(rust_string);
    }

    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            error!("failed to get Tokio runtime!");
            return 0;
        }
    };

    let result = rt.block_on(async { client.save_items(rust_uris).await });

    match result {
        Ok(status) => {
            let success = status.is_success();
            if !success {
                warn!("failed to save items with status: {}", status.as_str());
            }

            success as jboolean
        }
        Err(e) => {
            error!("save_items failed: {e}");
            0
        }
    }
}

#[unsafe(export_name = "Java_cc_tomko_outify_core_SpClient_deleteItems")]
pub extern "system" fn delete_items(
    mut env: JNIEnv,
    _class: JClass,
    uris: JObjectArray,
) -> jboolean {
    let client = get_client();

    let length = env.get_array_length(&uris).unwrap();
    let mut rust_uris = Vec::with_capacity(length as usize);

    for i in 0..length {
        let obj = env.get_object_array_element(&uris, i).unwrap();
        let jstr: JString = JString::from(obj);
        let rust_string: String = env.get_string(&jstr).unwrap().into();
        rust_uris.push(rust_string);
    }

    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            error!("failed to get Tokio runtime!");
            return 0;
        }
    };

    let result = rt.block_on(async { client.delete_items(rust_uris).await });

    match result {
        Ok(status) => {
            let success = status.is_success();
            if !success {
                warn!("failed to delee items with status: {}", status.as_str());
            }

            success as jboolean
        }
        Err(e) => {
            error!("delete_items failed: {e}");
            0
        }
    }
}

#[unsafe(export_name = "Java_cc_tomko_outify_core_SpClient_getUserTop")]
pub extern "system" fn get_user_top(mut env: JNIEnv, _class: JClass, r#type: JString) -> jstring {
    let client = get_client();

    let request_type: Option<String> = if r#type.is_null() {
        None
    } else {
        match env.get_string(&r#type) {
            Ok(t) => Some(t.into()),
            Err(e) => {
                error!("failed to get request type: {e}");
                return std::ptr::null_mut();
            }
        }
    };

    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            error!("failed to get Tokio runtime!");
            return std::ptr::null_mut();
        }
    };

    let result = rt.block_on(async { client.get_top(request_type).await });

    match result {
        Ok(result) => match serde_json::to_string(&result) {
            Ok(json) => match env.new_string(&json) {
                Ok(r) => r.into_raw(),
                Err(e) => {
                    error!("failed to create new jstring: {e}");
                    std::ptr::null_mut()
                }
            },
            Err(e) => {
                error!("failed to serialize result: {e}");
                std::ptr::null_mut()
            }
        },
        Err(e) => {
            error!("get_user_top failed: {e}");
            std::ptr::null_mut()
        }
    }
}

#[unsafe(export_name = "Java_cc_tomko_outify_core_SpClient_transferPlaybackDevice")]
pub extern "system" fn transfer_playback_device(
    mut env: JNIEnv,
    _class: JClass,
    device_id: JString,
) -> jboolean {
    let client = get_client();

    let device_id: String = match env.get_string(&device_id) {
        Ok(t) => t.into(),
        Err(e) => {
            error!("failed to get device id: {e}");
            return 0;
        }
    };

    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            error!("failed to get Tokio runtime!");
            return 0;
        }
    };

    let result = rt.block_on(async { client.transfer_playback(device_id).await });

    match result {
        Ok(result) => result.is_success() as jboolean,
        Err(e) => {
            error!("get_user_top failed: {e}");
            0
        }
    }
}

#[unsafe(export_name = "Java_cc_tomko_outify_core_SpClient_getDevices")]
pub extern "system" fn get_devices(env: JNIEnv, _class: JClass) -> jstring {
    let client = get_client();

    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            error!("failed to get Tokio runtime!");
            return std::ptr::null_mut();
        }
    };

    let result = rt.block_on(async { client.get_devices().await });
    match result {
        Ok(devices) => match serde_json::to_string(&devices) {
            Ok(json) => match env.new_string(&json) {
                Ok(r) => r.into_raw(),
                Err(e) => {
                    error!("failed to create new jstring: {e}");
                    std::ptr::null_mut()
                }
            },
            Err(e) => {
                error!("failed to serialize result: {e}");
                std::ptr::null_mut()
            }
        },
        Err(e) => {
            error!("failed to get devices: {e}");
            return std::ptr::null_mut();
        }
    }
}

#[unsafe(export_name = "Java_cc_tomko_outify_core_SpClient_isOAuthAuthenticated")]
pub extern "system" fn is_oauth_authenticated(_env: JNIEnv, _class: JClass) -> jboolean {
    let client = get_client();

    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            error!("failed to get Tokio runtime!");
            return 0 as jboolean;
        }
    };

    let result = rt.block_on(async { client.is_oauth_authenticated().await });

    result as jboolean
}

#[unsafe(export_name = "Java_cc_tomko_outify_core_SpClient_addToPlaylist")]
pub extern "system" fn add_to_playlist(
    mut env: JNIEnv,
    _class: JClass,
    playlist_id: JString,
    uris: JObjectArray,
) -> jboolean {
    let client = get_client();

    let playlist_id: String = match env.get_string(&playlist_id) {
        Ok(p) => p.into(),
        Err(e) => {
            error!("failed to get playlist_id: {e}");
            return 0 as jboolean;
        }
    };

    let length = env.get_array_length(&uris).unwrap();
    let mut rust_uris = Vec::with_capacity(length as usize);

    for i in 0..length {
        let obj = env.get_object_array_element(&uris, i).unwrap();
        let jstr: JString = JString::from(obj);
        let rust_string: String = env.get_string(&jstr).unwrap().into();
        rust_uris.push(rust_string);
    }

    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            error!("failed to get Tokio runtime!");
            return 0;
        }
    };

    let result = rt.block_on(async { client.add_to_playlist(playlist_id, rust_uris).await });

    match result {
        Ok(status) => {
            let success = status.is_success();
            if !success {
                warn!("failed to add to playlist with status: {}", status.as_str());
            }

            success as jboolean
        }
        Err(e) => {
            error!("add_to_playlist failed: {e}");
            0
        }
    }
}

#[unsafe(export_name = "Java_cc_tomko_outify_core_SpClient_deleteFromPlaylist")]
pub extern "system" fn delete_from_playlist(
    mut env: JNIEnv,
    _class: JClass,
    playlist_id: JString,
    uris: JObjectArray,
) -> jboolean {
    let client = get_client();

    let playlist_id: String = match env.get_string(&playlist_id) {
        Ok(p) => p.into(),
        Err(e) => {
            error!("failed to get playlist_id: {e}");
            return 0 as jboolean;
        }
    };

    let length = env.get_array_length(&uris).unwrap();
    let mut rust_uris = Vec::with_capacity(length as usize);

    for i in 0..length {
        let obj = env.get_object_array_element(&uris, i).unwrap();
        let jstr: JString = JString::from(obj);
        let rust_string: String = env.get_string(&jstr).unwrap().into();
        rust_uris.push(rust_string);
    }

    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            error!("failed to get Tokio runtime!");
            return 0;
        }
    };

    let result = rt.block_on(async { client.delete_from_playlist(playlist_id, rust_uris).await });

    match result {
        Ok(status) => {
            let success = status.is_success();
            if !success {
                warn!(
                    "failed to delete from playlist with status: {}",
                    status.as_str()
                );
            }

            success as jboolean
        }
        Err(e) => {
            error!("delete_from_playlist failed: {e}");
            0
        }
    }
}

// Searches for tracks using context
#[unsafe(no_mangle)]
#[deprecated]
pub extern "system" fn Java_cc_tomko_outify_core_SpClient_searchContext(
    mut env: JNIEnv,
    _class: JClass,
    query: JString,
    pages: jint,
    page_offset: jint,
) -> jstring {
    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            error!("failed to get Tokio runtime!");
            return std::ptr::null_mut();
        }
    };

    let query: String = match env.get_string(&query) {
        Ok(q) => q.into(),
        Err(e) => {
            error!("failed to get query as string: {}", e);
            return std::ptr::null_mut();
        }
    };

    let page_offset = page_offset as usize;
    let pages = pages as usize;

    let json_res = rt.block_on(async {
        let uri = format!("spotify:search:{}", query);

        match crate::spclient::get_context(uri.as_str()).await {
            Ok(context) => {
                let uris: Vec<String> = context
                    .pages
                    .iter()
                    .flat_map(|page| &page.tracks)
                    .filter_map(|track| track.uri.clone())
                    .collect();

                match serde_json::to_string(&uris) {
                    Ok(s) => Ok(s),
                    Err(e) => {
                        error!("failed to serialize uris: {}", e);
                        Err(())
                    }
                }
            }
            Err(e) => {
                error!("failed to get context: {}", e);
                Err(())
            }
        }
    });

    let json = match json_res {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };

    match env.new_string(json) {
        Ok(jstr) => jstr.into_raw(),
        Err(e) => {
            error!("JNI new_string failed: {:?}", e);
            std::ptr::null_mut()
        }
    }
}

// Gets user collection
// Query can be used to get liked songs from artist
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_SpClient_getUserCollection(
    mut env: JNIEnv,
    _class: JClass,
    query: JString,
) -> jstring {
    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            error!("failed to get Tokio runtime!");
            return std::ptr::null_mut();
        }
    };

    let query: Option<String> = if query.is_null() {
        None
    } else {
        match env.get_string(&query) {
            Ok(js) => Some(js.into()),
            Err(e) => {
                error!("failed to get query uri: {}", e);
                return std::ptr::null_mut();
            }
        }
    };

    let user_id = match with_session(|session| session.username()) {
        Ok(u) => u,
        Err(e) => {
            error!("Failed to get user_id: {e}");
            return std::ptr::null_mut();
        }
    };

    let json_res = rt.block_on(async {
        let uri = match query {
            Some(ref q) => format!("spotify:user:{}:collection{}", user_id, q),
            None => format!("spotify:user:{}:collection", user_id),
        };

        match crate::spclient::get_context(&uri).await {
            Ok(context) => {
                let uris: Vec<String> = context
                    .pages
                    .iter()
                    .flat_map(|page| page.tracks.iter())
                    .map(|track| track.uri().to_string())
                    .collect();

                match serde_json::to_string(&uris) {
                    Ok(s) => Ok(s),
                    Err(e) => {
                        error!("failed to serialize uris: {}", e);
                        Err(())
                    }
                }
            }
            Err(e) => {
                error!("failed to get context: {}", e);
                Err(())
            }
        }
    });

    let json = match json_res {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };

    match env.new_string(json) {
        Ok(jstr) => jstr.into_raw(),
        Err(e) => {
            error!("JNI new_string failed: {:?}", e);
            std::ptr::null_mut()
        }
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_SpClient_getRootlist(
    mut env: JNIEnv,
    _class: JClass,
) -> jobjectArray {
    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            error!("failed to get Tokio runtime!");
            return std::ptr::null_mut();
        }
    };

    let uris: Vec<String> = rt.block_on(async {
        match crate::spclient::get_rootlist().await {
            Ok(bytes) => {
                let mut uris = Vec::new();
                let data = String::from_utf8_lossy(&bytes);
                let pattern = Regex::new(r"spotify:playlist:[A-Za-z0-9]+").unwrap();

                for mat in pattern.find_iter(&data) {
                    uris.push(mat.as_str().to_string());
                }

                uris
            }
            Err(_) => Vec::new(),
        }
    });

    let string_class = env.find_class("java/lang/String").unwrap();
    let array = env
        .new_object_array(uris.len() as i32, string_class, JObject::null())
        .unwrap();

    for (i, uri) in uris.iter().enumerate() {
        let jstr = env.new_string(uri).unwrap();
        env.set_object_array_element(&array, i as i32, jstr)
            .unwrap();
    }

    array.into_raw()
}

#[unsafe(export_name = "Java_cc_tomko_outify_core_SpClient_getRadioForTrack")]
pub extern "system" fn get_radio_for_track(
    mut env: JNIEnv,
    _class: JClass,
    track_uri: JString,
) -> jstring {
    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            error!("failed to get Tokio runtime!");
            return std::ptr::null_mut();
        }
    };

    let track_uri_raw: String = match env.get_string(&track_uri) {
        Ok(js) => js.into(),
        Err(e) => {
            error!("failed to get track_uri: {}", e);
            return std::ptr::null_mut();
        }
    };

    let outify_uri = OutifyUri::from_uri(&track_uri_raw);
    let uri_string = outify_uri.to_uri();

    let track_uri = match SpotifyUri::from_uri(&uri_string.as_str()) {
        Ok(u) => u,
        Err(e) => {
            error!("failed to convert uri: {e}");
            return std::ptr::null_mut();
        }
    };

    let json_opt = rt.block_on(async {
        match crate::spclient::get_radio_for_track(&track_uri).await {
            Ok(bytes) => match String::from_utf8(bytes.to_vec()) {
                Ok(s) => Some(s),
                Err(e) => {
                    error!("failed to convert to string: {}", e);
                    None
                }
            },
            Err(e) => {
                error!("request failed: {}", e);
                None
            }
        }
    });

    let json = match json_opt {
        Some(s) => s,
        None => return std::ptr::null_mut(),
    };

    match env.new_string(json) {
        Ok(j) => j.into_raw(),
        Err(e) => {
            error!("failed to convert to jstring: {}", e);
            std::ptr::null_mut()
        }
    }
}

#[unsafe(export_name = "Java_cc_tomko_outify_core_SpClient_getLyrics")]
pub extern "system" fn get_lyrics_for_track(
    mut env: JNIEnv,
    _class: JClass,
    track_id: JString,
) -> jstring {
    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            error!("failed to get Tokio runtime!");
            return std::ptr::null_mut();
        }
    };

    let track_id_raw: String = match env.get_string(&track_id) {
        Ok(js) => js.into(),
        Err(e) => {
            error!("failed to get track_id: {}", e);
            return std::ptr::null_mut();
        }
    };

    let track_id = match SpotifyId::from_base62(&track_id_raw.as_str()) {
        Ok(u) => u,
        Err(e) => {
            error!("failed to convert uri: {e}");
            return std::ptr::null_mut();
        }
    };

    let json_opt = rt.block_on(async {
        match crate::spclient::get_lyrics(&track_id).await {
            Ok(bytes) => match String::from_utf8(bytes.to_vec()) {
                Ok(s) => Some(s),
                Err(e) => {
                    error!("failed to convert to string: {}", e);
                    None
                }
            },
            Err(e) => {
                error!("request failed: {}", e);
                None
            }
        }
    });

    let json = match json_opt {
        Some(s) => s,
        None => return std::ptr::null_mut(),
    };

    match env.new_string(json) {
        Ok(j) => j.into_raw(),
        Err(e) => {
            error!("failed to convert to jstring: {}", e);
            std::ptr::null_mut()
        }
    }
}

/// Starts the OAuth flow for SpotifyClient and returns the authorization URL
#[unsafe(export_name = "Java_cc_tomko_outify_core_SpClient_startOAuthFlow")]
pub extern "system" fn start_oauth_flow(env: JNIEnv, _class: JClass) -> jstring {
    let client = get_client();

    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            error!("failed to get Tokio runtime!");
            return std::ptr::null_mut();
        }
    };

    let auth_url_result = rt.block_on(async { client.start_oauth_flow().await });

    let auth_url = match auth_url_result {
        Ok(url) => url,
        Err(e) => {
            error!("Failed to start OAuth flow: {e}");
            return std::ptr::null_mut();
        }
    };

    match env.new_string(auth_url) {
        Ok(java_str) => java_str.into_raw(),
        Err(e) => {
            error!("Failed to create Java string: {:?}", e);
            std::ptr::null_mut()
        }
    }
}

#[unsafe(export_name = "Java_cc_tomko_outify_core_SpClient_completeOAuthFlow")]
pub extern "system" fn complete_oauth_flow(
    mut env: JNIEnv,
    _class: JClass,
    code: JString,
) -> jstring {
    let client = get_client();

    let code: String = match env.get_string(&code) {
        Ok(js) => js.into(),
        Err(e) => {
            error!("JNI failed to read code: {}", e);
            return spclient_make_error_json(&env, "authentication", "Failed to read OAuth code");
        }
    };

    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            error!("failed to get Tokio runtime!");
            return spclient_make_error_json(&env, "unknown", "Tokio runtime not initialized");
        }
    };

    let result = rt.block_on(async { client.complete_oauth_flow(code).await });

    match result {
        Ok(token) => {
            debug!("OAuth flow completed successfully, token: {}", token.access_token);
            spclient_make_success_json(&env)
        }
        Err(e) => {
            error!("OAuth flow completion failed: {e}");
            let err_type = classify_spclient_error(&e);
            spclient_make_error_json(&env, err_type, &e.to_string())
        }
    }
}

fn spclient_make_error_json(env: &JNIEnv, error_type: &str, message: &str) -> jstring {
    let json = format!(r#"{{"error":{{"type":"{}","message":"{}"}}}}"#, error_type, message);
    match env.new_string(json) {
        Ok(s) => s.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

fn spclient_make_success_json(env: &JNIEnv) -> jstring {
    match env.new_string(r#"{"success":true}"#) {
        Ok(s) => s.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

fn classify_spclient_error(err: &crate::spotify::error::SpotifyApiError) -> &'static str {
    let msg = err.to_string().to_lowercase();
    if msg.contains("unavailable") || msg.contains("service") {
        "service_unavailable"
    } else if msg.contains("auth") || msg.contains("token") || msg.contains("credential") || msg.contains("unauthorized") {
        "authentication_error"
    } else if msg.contains("rate") {
        "rate_limit"
    } else {
        "unknown"
    }
}

#[unsafe(export_name = "Java_cc_tomko_outify_core_SpClient_logout")]
pub extern "system" fn logout(_env: JNIEnv, _class: JClass) -> jboolean {
    let client = get_client();
    match client.remove_token() {
        Ok(_) => {
            info!("Spotify Client credentials deleted!");
            1
        },
        Err(_) => {
            0
        },
    }
}
