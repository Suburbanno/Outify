use jni::{
    JNIEnv,
    objects::{GlobalRef, JClass, JMethodID, JObject, JValue},
    sys::jboolean,
};
use librespot_metadata::{Album, Artist, Track};
use serde::Serialize;

// Serializable Track object
#[derive(Serialize)]
pub struct TrackJson {
    id: String,
    uri: String,
    name: String,
    album: AlbumJson,       
    artists: Vec<ArtistJson>,
    duration: i32,
    explicit: bool,
}

impl From<&Track> for TrackJson {
    fn from(track: &Track) -> Self {
        Self {
            id: track.id.to_id(),
            uri: track.id.to_uri(),
            name: track.name.clone(),
            album: AlbumJson::from(&track.album),
            artists: track.artists.iter().map(ArtistJson::from).collect(),
            duration: track.duration,
            explicit: track.is_explicit,
        }
    }
}

// Serializable Artist object
#[derive(Serialize)]
pub struct ArtistJson {
    id: String,
    uri: String,
    name: String,
    popularity: i32,
}

impl From<&Artist> for ArtistJson {
    fn from(artist: &Artist) -> Self {
        Self {
            id: artist.id.to_id(),
            uri: artist.id.to_uri(),
            name: artist.name.clone(),
            popularity: artist.popularity,
        }
    }
}

// Serializable Album object
#[derive(Serialize)]
pub struct AlbumJson {
    id: String,
    uri: String,
    name: String,
    artists: Vec<ArtistJson>,
    popularity: i32,
}

impl From<&Album> for AlbumJson {
    fn from(album: &Album) -> Self {
        Self {
            id: album.id.to_id(),
            uri: album.id.to_uri(),
            name: album.name.clone(),
            artists: album.artists.iter().map(ArtistJson::from).collect(),
            popularity: album.popularity,
        }
    }
}

// Converts native track into kotlin compatible NativeTrack
pub fn convert_track(track: &Track) -> Option<JObject> {
    let jvm = match crate::JVM.get() {
        Some(j) => j,
        None => {
            warn!("Cannot get class NativeTrack because JVM is not set!",);
            return None;
        }
    };

    let mut env = match jvm.get_env() {
        Ok(e) => e,
        Err(e) => {
            warn!("failed to get env: {}", e);
            return None;
        }
    };

    let class = env
        .find_class("cc/tomko/outify/data/native/NativeTrack")
        .expect("NativeTrack class not found");

    let id = env.new_string(track.id.to_uri()).unwrap();
    let name = env.new_string(track.name.clone()).unwrap();

    Some(
        env.new_object(
            class,
            "(Ljava/lang/String;Ljava/lang/String;IIZ)V",
            &[
                JValue::Object(&id),
                JValue::Object(&name),
                JValue::Int(track.duration),
                JValue::Int(track.popularity),
                JValue::Bool(track.is_explicit as jboolean),
            ],
        )
        .expect("Failed to create NativeTrack"),
    )
}
