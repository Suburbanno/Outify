use bytes::Bytes;
use librespot_core::{spclient::{SpClient, SpClientResult}, SpotifyId, SpotifyUri};
use librespot_protocol::context::Context;

use crate::session::with_session;

// Retrieves the context from given URI
pub async fn get_context(uri: &str) -> Result<Context, librespot_core::error::Error> {
    let session = match with_session(|s| s.clone()) {
        Ok(s) => s,
        Err(e) => {
            error!("Failed to get session: {}", e);
            return Err(librespot_core::Error::internal("Failed to get session"));
        }
    };
    let spclient = session.spclient();
    spclient.get_context(uri).await
}

pub async fn get_rootlist() -> SpClientResult {
    let session = match with_session(|s| s.clone()) {
        Ok(s) => s,
        Err(e) => {
            error!("Failed to get session: {}", e);
            return Err(librespot_core::Error::internal("Failed to get session"));
        }
    };
    let spclient = session.spclient();
    spclient.get_rootlist(0, None).await
}

pub async fn get_radio_for_track(track_uri: &SpotifyUri) -> SpClientResult {
    let session = match with_session(|s| s.clone()) {
        Ok(s) => s,
        Err(e) => {
            error!("Failed to get session: {}", e);
            return Err(librespot_core::Error::internal("Failed to get session"));
        }
    };

    let spclient = session.spclient();
    spclient.get_radio_for_track(track_uri).await
}

pub async fn get_lyrics(track_id: &SpotifyId) -> SpClientResult {
    let session = match with_session(|s| s.clone()) {
        Ok(s) => s,
        Err(e) => {
            error!("Failed to get session: {}", e);
            return Err(librespot_core::Error::internal("Failed to get session"));
        }
    };

    let spclient = session.spclient();
    spclient.get_lyrics(track_id).await
}
