use jni::{
    objects::{JClass, JString},
    sys::jstring,
};
use librespot_core::SpotifyUri;
use librespot_metadata::Metadata;

// Called when we need to get extra metadata from Spotify URI
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_data_Metadata_getMetadata(
    mut env: jni::JNIEnv<'_>,
    _this: JClass<'_>,
    juri: JString<'_>,
) -> jstring {
    let uri: String = match env.get_string(&juri) {
        Ok(u) => u.into(),
        Err(e) => {
            error!("failed to get metadata: {}", e);
            return std::ptr::null_mut();
        }
    };

    let spotify_uri = match SpotifyUri::from_uri(uri.as_str()) {
        Ok(u) => u,
        Err(e) => {
            error!("failed to parse SpotifyURI: {}", e);
            return std::ptr::null_mut();
        }
    };

    // Getting session
    let session = match crate::session::SESSION.get() {
        Some(s) => s,
        None => {
            error!("failed to retrieve session: not initialized");
            return std::ptr::null_mut();
        }
    };

    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            error!("failed to retrieve tokio runtime");
            return std::ptr::null_mut();
        }
    };

    let maybe_json: Option<String> = rt.block_on(async {
        match librespot_metadata::Track::get(session, &spotify_uri).await {
            Ok(metadata) => {
                let track = crate::jni_utils::native_metadata::TrackJson::from(&metadata);
                match serde_json::to_string(&track) {
                    Ok(j) => Some(j),
                    Err(e) => {
                        error!("getMetadata: serde_json: {}", e);
                        None
                    }
                }
            }
            Err(e) => {
                error!("failed to fetch metadata: {}", e);
                None
            }
        }
    });

    let json = match maybe_json {
        Some(j) => j,
        None => {
            error!("failed to get json from maybe_json");
            return std::ptr::null_mut()
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

