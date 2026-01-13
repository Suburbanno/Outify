#![crate_name = "librespot_api"]

mod errors;
pub mod oauth;

pub use errors::Error;

use std::env;

use librespot_core::{
    authentication::Credentials, config::SessionConfig, session::Session, token::Token,
};
use librespot_oauth::{OAuthClientBuilder, OAuthToken};
use oauth2::{url::Url};

pub const SCOPES: &[&str] = &[
    "streaming",
    "user-read-playback-state",
    "user-modify-playback-state",
    "user-read-currently-playing",
];

pub async fn oauth_login(access_token: &str) -> Result<Token, Error> {
    //let mut builder = env_logger::Builder::new();

    let session_config = SessionConfig::default();
    let session = Session::new(session_config, None);
    let credentials = Credentials::with_access_token(access_token);

    println!("Connecting with token..");
    session.connect(credentials, false).await?;

    let token = session.token_provider().get_token(&SCOPES.join(",")).await?;
    println!("Got me a token: {token:#?}");

    Ok(token)
}
