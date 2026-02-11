use jni::{
    JNIEnv,
    objects::{JClass, JString},
    sys::{jint, jstring},
};
use librespot_core::{SpotifyUri, spclient::SpClient};
use librespot_metadata::{Metadata, Track};
use oauth2::reqwest;

#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_SpClient_search(
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

    let user_id = match crate::session::SESSION.get() {
        Some(s) => s.username(),
        None => {
            error!("failed to get user_id");
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
