use std::collections::HashMap;

use base64::{engine::general_purpose, Engine};
use jni::{
    JNIEnv,
    objects::{GlobalRef, JClass, JMethodID, JObject, JValue},
    sys::jboolean,
};
use librespot_metadata::{
    Album, Artist, Playlist, Track,
    image::Image,
    playlist::{
        attribute::{PlaylistAttributes, PlaylistItemAttributes},
        item::PlaylistItem,
    },
};
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
    files: Vec<FileJson>,
}

impl From<&Track> for TrackJson {
    fn from(track: &Track) -> Self {
        let files = track.files.0.iter().map(|(format, file_id)| {
            FileJson {
                r#type: format!("{:?}", format),
                id: file_id.to_base16(),
            }
        }).collect();

        Self {
            id: track.id.to_id(),
            uri: track.id.to_uri(),
            name: track.name.clone(),
            album: AlbumJson::from(&track.album),
            artists: track.artists.iter().map(ArtistJson::from).collect(),
            popularity: track.popularity,
            duration: track.duration,
            explicit: track.is_explicit,
            files,
        }
    }
}

#[derive(Serialize)]
pub struct FileJson {
    r#type: String,
    id: String,
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
    covers: Vec<ImageJson>,
    albums: Vec<String>,
}

impl From<&Artist> for ArtistJson {
    fn from(artist: &Artist) -> Self {
        let tracks = artist
            .top_tracks
            .0
            .iter()
            .flat_map(|top| top.tracks.iter())
            .map(|track| track.to_uri())
            .collect();

        Self {
            id: artist.id.to_id(),
            uri: artist.id.to_uri(),
            name: artist.name.clone(),
            popularity: artist.popularity,
            portraits: artist.portraits.iter().map(ImageJson::from).collect(),
            tracks: tracks,
            covers: artist.portrait_group.iter().map(ImageJson::from).collect(),
            albums: artist
                .albums
                .0
                .iter()
                .flat_map(|group| group.0.0.iter())
                .map(|uri| uri.to_uri())
                .collect(),
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
    covers: Vec<ImageJson>,
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
