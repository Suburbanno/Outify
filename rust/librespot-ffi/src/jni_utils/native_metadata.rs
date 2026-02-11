use jni::{
    JNIEnv,
    objects::{GlobalRef, JClass, JMethodID, JObject, JValue},
    sys::jboolean,
};
use librespot_metadata::{image::Image, Album, Artist, Track};
use serde::Serialize;

// Serializable Track object
#[derive(Serialize)]
pub struct TrackJson {
    id: String,
    uri: String,
    name: String,
    album: AlbumJson,       
    artists: Vec<ArtistJson>,
    popularity: i32,
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
            popularity: track.popularity,
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
    portraits: Vec<ImageJson>,
    tracks: Vec<String>, // Just raw SpotifyUris
    covers: Vec<ImageJson>
}

impl From<&Artist> for ArtistJson {
    fn from(artist: &Artist) -> Self {
        let tracks = artist.top_tracks.0.iter()
            .flat_map(|top| top.tracks.iter())
            .map(|track| track.to_uri())
            .collect();

        for ele in artist.portraits.iter() {
            info!("Portrait uri: {}", ele.id.to_string());
        }

        for ele in artist.portrait_group.iter() {
            info!("Portrait g uri: {}", ele.id.to_string());
        }

        Self {
            id: artist.id.to_id(),
            uri: artist.id.to_uri(),
            name: artist.name.clone(),
            popularity: artist.popularity,
            portraits: artist.portraits.iter().map(ImageJson::from).collect(),
            tracks: tracks,
            covers: artist.portrait_group.iter().map(ImageJson::from).collect(),
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
    tracks: Vec<String>, // Just Spotify URI
    covers: Vec<ImageJson>
}

impl From<&Album> for AlbumJson {
    fn from(album: &Album) -> Self {
        Self {
            id: album.id.to_id(),
            uri: album.id.to_uri(),
            name: album.name.clone(),
            artists: album.artists.iter().map(ArtistJson::from).collect(),
            popularity: album.popularity,
            tracks: album.tracks().map(|uri| uri.to_uri()).collect(),
            covers: album.covers.iter().map(ImageJson::from).collect(),
        }
    }
}

#[derive(Serialize)]
pub struct ImageJson {
    pub uri: String,
    pub size: i8, // 0 -> 3 values ranging by size
    pub width: i32,
    pub height: i32,
}

impl From<&Image> for ImageJson {
    fn from(img: &Image) -> Self {
        let size = match img.size {
            librespot_metadata::image::ImageSize::DEFAULT => 0,
            librespot_metadata::image::ImageSize::SMALL => 1,
            librespot_metadata::image::ImageSize::LARGE => 2,
            librespot_metadata::image::ImageSize::XLARGE => 3,
        };

        Self {
            uri: img.id.to_base16(),
            size,
            width: img.width,
            height: img.height,
        }
    }
}
