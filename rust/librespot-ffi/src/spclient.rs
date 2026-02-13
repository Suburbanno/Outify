use bytes::Bytes;
use librespot_core::{SpotifyUri, spclient::SpClient};
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
