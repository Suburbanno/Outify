use jni::{
    JNIEnv,
    objects::{JClass, JObject, JObjectArray, JString},
    sys::{jboolean, jint, jobjectArray, jstring},
};
use librespot_core::{SpotifyUri, spclient::SpClient};
use librespot_metadata::{Metadata, Track};
use oauth2::reqwest;
use regex::Regex;

use crate::{jni_utils::vec_to_jstring_array, session::with_session, spotify::client::get_client};

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
