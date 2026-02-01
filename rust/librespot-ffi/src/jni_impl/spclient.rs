use jni::{
    JNIEnv,
    objects::{JClass, JString},
    sys::jstring,
};
use librespot_core::spclient::SpClient;

#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_SpClient_search(
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

    let query: String = match env.get_string(&query) {
        Ok(q) => q.into(),
        Err(e) => {
            error!("failed to get query as string: {}", e);
            return std::ptr::null_mut();
        }
    };

    let json_res = rt.block_on(async {
        let uri = format!("spotify:search:{}", query);

        match crate::spclient::get_context(uri.as_str()).await {
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
pub extern "system" fn Java_cc_tomko_outify_core_SpClient_getLikedSongs(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            error!("failed to get Tokio runtime!");
            return std::ptr::null_mut();
        }
    };

    let user_id = match crate::session::SESSION.get() {
        Some(s) => {
            s.username()
        }
        None => {
            error!("failed to get user_id");
            return std::ptr::null_mut()
        }
    };

    let json_res = rt.block_on(async {
        let uri = format!("spotify:user:{}:collection", user_id);

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
