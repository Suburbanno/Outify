use jni::{
    objects::{JClass, JString},
    sys::jstring,
};
use librespot_core::{Session, SpotifyUri};
use librespot_metadata::Metadata;

use crate::session::with_session;

// From librespot_metadata
const SPOTIFY_ITEM_TYPE_ALBUM: &str = "album";
const SPOTIFY_ITEM_TYPE_ARTIST: &str = "artist";
const SPOTIFY_ITEM_TYPE_EPISODE: &str = "episode";
const SPOTIFY_ITEM_TYPE_PLAYLIST: &str = "playlist";
const SPOTIFY_ITEM_TYPE_SHOW: &str = "show";
const SPOTIFY_ITEM_TYPE_TRACK: &str = "track";
const SPOTIFY_ITEM_TYPE_LOCAL: &str = "local";
const SPOTIFY_ITEM_TYPE_UNKNOWN: &str = "unknown";

#[unsafe(no_mangle)]
#[export_name = "Java_cc_tomko_outify_data_metadata_Metadata_getNativeMetadata"]
pub extern "system" fn get_native_metadata(
    mut env: jni::JNIEnv,
    _this: JClass,
    juri: JString,
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
            let _ = env.throw_new(
                "java/lang/IllegalArgumentException",
                format!("Invalid Spotify URI: {}", e),
            );
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

    let maybe_json: Option<String> = match with_session(|session| {
        rt.block_on(async move {
            match spotify_uri.item_type() {
                SPOTIFY_ITEM_TYPE_TRACK => get_track_metadata(&session, &spotify_uri).await,
                SPOTIFY_ITEM_TYPE_ALBUM => get_album_metadata(&session, &spotify_uri).await,
                SPOTIFY_ITEM_TYPE_ARTIST => get_artist_metadata(&session, &spotify_uri).await,
                SPOTIFY_ITEM_TYPE_PLAYLIST => get_playlist_metadata(&session, &spotify_uri).await,
                &_ => {
                    info!("Unknown item type!");
                    None
                }
            }
        })
    }) {
        Ok(val) => val,
        Err(e) => {
            error!("Failed to get session: {}", e);
            return std::ptr::null_mut(); // return null on error
        }
    };

    let json = match maybe_json {
        Some(j) => j,
        None => {
            error!("failed to get json from maybe_json");
            return std::ptr::null_mut();
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

// Retrieves the album metadata as JSON
async fn get_album_metadata(session: &Session, spotify_uri: &SpotifyUri) -> Option<String> {
    match librespot_metadata::Album::get(session, &spotify_uri).await {
        Ok(metadata) => {
            let album = crate::jni_utils::native_metadata::AlbumJson::from(&metadata);
            convert_to_string(&album)
        }
        Err(e) => {
            error!("failed to fetch album metadata: {}", e);
            None
        }
    }
}

// Retrieves the album metadata as JSON
async fn get_track_metadata(session: &Session, spotify_uri: &SpotifyUri) -> Option<String> {
    match librespot_metadata::Track::get(session, &spotify_uri).await {
        Ok(metadata) => {
            let track = crate::jni_utils::native_metadata::TrackJson::from(&metadata);
            convert_to_string(&track)
        }
        Err(e) => {
            error!("failed to fetch album metadata: {}", e);
            None
        }
    }
}

// Retrieves the artist metadata as JSON
async fn get_artist_metadata(session: &Session, spotify_uri: &SpotifyUri) -> Option<String> {
    match librespot_metadata::Artist::get(session, &spotify_uri).await {
        Ok(metadata) => {
            let artist = crate::jni_utils::native_metadata::ArtistJson::from(&metadata);
            convert_to_string(&artist)
        }
        Err(e) => {
            error!("failed to fetch artist metadata: {}", e);
            None
        }
    }
}

async fn get_playlist_metadata(session: &Session, spotify_uri: &SpotifyUri) -> Option<String> {
    match librespot_metadata::Playlist::get(session, &spotify_uri).await {
        Ok(metadata) => {
            let playlist = crate::metadata::playlist::PlaylistJson::from(&metadata);
            convert_to_string(&playlist)
        }
        Err(e) => {
            error!("failed to fetch playlist metadata: {}", e);
            None
        }
    }
}

fn convert_to_string<T: serde::Serialize>(metadata: &T) -> Option<String> {
    match serde_json::to_string(&metadata) {
        Ok(json) => Some(json),
        Err(e) => {
            error!("serde_json: {}", e);
            None
        }
    }
}
