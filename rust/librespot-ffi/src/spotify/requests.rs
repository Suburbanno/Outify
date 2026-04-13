use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Page<T> {
    pub href: String,
    pub limit: i32,
    pub next: Option<String>,
    pub offset: i32,
    pub previous: Option<String>,
    pub total: i32,
    pub items: Vec<T>,
}

#[derive(Serialize)]
pub struct AddItemRequest {
    pub uris: Vec<String>,
    pub position: Option<u32>,
}

#[derive(Serialize)]
pub struct RemoveItemRequest {
    pub items: Vec<RemoveItem>,
}

#[derive(Serialize)]
pub struct RemoveItem {
    pub uri: String,
}

// https://developer.spotify.com/documentation/web-api/reference/get-users-top-artists-and-tracks
#[derive(Serialize)]
pub struct UserTopRequest {
    pub r#type: String,
    // pub limit: Option<u32>,
    // pub offset: Option<u32>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum ArtistOrTrack {
    Artist(ArtistObject),
    Track(TrackObject),
}

pub type ArtistsOrTracksPage = Page<ArtistOrTrack>;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ArtistObject {
    pub external_urls: Option<ExternalUrls>,
    pub followers: Option<Followers>,
    pub genres: Option<Vec<String>>,
    pub href: Option<String>,
    pub id: Option<String>,
    pub images: Option<Vec<ImageObject>>,
    pub name: String,
    pub popularity: Option<i32>,
    #[serde(rename = "type")]
    pub object_type: String, // usually "artist"
    pub uri: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TrackObject {
    pub album: Option<AlbumObject>,
    pub artists: Option<Vec<SimplifiedArtistObject>>,
    pub available_markets: Option<Vec<String>>,
    pub disc_number: Option<i32>,
    pub duration_ms: Option<i32>,
    pub explicit: Option<bool>,
    pub external_ids: Option<ExternalIds>,
    pub external_urls: Option<ExternalUrls>,
    pub href: Option<String>,
    pub id: Option<String>,
    pub is_playable: Option<bool>,
    pub linked_from: Option<LinkedFromObject>,
    pub restrictions: Option<RestrictionsObject>,
    pub name: String,
    pub popularity: Option<i32>,
    pub preview_url: Option<String>,
    pub track_number: Option<i32>,
    #[serde(rename = "type")]
    pub object_type: String, // usually "track"
    pub uri: Option<String>,
    pub is_local: Option<bool>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ExternalUrls {
    pub spotify: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Followers {
    pub href: Option<String>,
    pub total: Option<i32>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ImageObject {
    pub url: String,
    pub height: Option<i32>,
    pub width: Option<i32>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ExternalIds {
    pub isrc: Option<String>,
    pub ean: Option<String>,
    pub upc: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LinkedFromObject {
    pub external_urls: Option<ExternalUrls>,
    pub href: Option<String>,
    pub id: Option<String>,
    pub uri: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RestrictionsObject {
    pub reason: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AlbumObject {
    pub href: Option<String>,
    pub id: Option<String>,
    pub name: Option<String>,
    pub album_type: Option<String>,
    pub album_group: Option<String>,
    pub artists: Option<Vec<SimplifiedArtistObject>>,
    pub images: Option<Vec<ImageObject>>,
    pub uri: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SimplifiedArtistObject {
    pub external_urls: Option<ExternalUrls>,
    pub href: Option<String>,
    pub id: Option<String>,
    pub name: Option<String>,
    #[serde(rename = "type")]
    pub object_type: Option<String>, // usually "artist"
    pub uri: Option<String>,
}
