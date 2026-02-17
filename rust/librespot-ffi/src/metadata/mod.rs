pub mod playlist;

use base64::{Engine as _, engine::general_purpose};
use librespot_core::SpotifyId;

pub fn bytes_to_base64(b: &[u8]) -> String {
    general_purpose::STANDARD.encode(b)
}
