use bytes::Bytes;
use librespot_core::{spclient::SpClient, SpotifyUri};
use librespot_protocol::context::Context;

// Retrieves the context from given URI
pub async fn get_context(uri: &str) -> Result<Context, librespot_core::error::Error> {
    let session = match crate::session::SESSION.get() {
        Some(s) => s,
        None => {
            error!("failed to get SpClient: session uninitialized");
            return Err(librespot_core::error::Error::unavailable(
                "session uninitialized",
            ));
        }
    };

    let spclient: &SpClient = session.spclient();
    spclient.get_context(uri).await
}

pub async fn get_track_metadata(uri: &SpotifyUri) -> Result<Bytes, librespot_core::error::Error> {
    let session = match crate::session::SESSION.get() {
        Some(s) => s,
        None => {
            error!("failed to get SpClient: session uninitialized");
            return Err(librespot_core::error::Error::unavailable(
                "session uninitialized",
            ));
        }
    };

    let spclient: &SpClient = session.spclient();
    spclient.get_track_metadata(uri).await
}
