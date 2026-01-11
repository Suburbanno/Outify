#![crate_name = "librespot_api"]

mod errors;
mod oauth;

pub use errors::Error;

use std::env;

use librespot_core::{
    authentication::Credentials, config::SessionConfig, session::Session, token::Token,
};
use librespot_oauth::{OAuthClientBuilder, OAuthToken};
use oauth2::{url::Url};

const SPOTIFY_CLIENT_ID: &str = "65b708073fc0480ea92a077233ca87bd";
const SPOTIFY_REDIRECT_URI: &str = "http://127.0.0.1:8898/login";

const SCOPES: &[&str] = &[
    "streaming",
    "user-read-playback-state",
    "user-modify-playback-state",
    "user-read-currently-playing",
];

pub async fn oauth_get_auth_url() -> Result<Url, Error> {
    let client = OAuthClientBuilder::new(SPOTIFY_CLIENT_ID, SPOTIFY_REDIRECT_URI, SCOPES.to_vec())
        .build()
        .map_err(|e| Error::internal(format!("Unable to build OAuth client: {e}")))?;

    let (auth_url, _) = client.set_auth_url();
    println!("Authorize at: {}", auth_url);

    Ok(auth_url)
}

pub async fn oauth_get_access_token() -> Result<OAuthToken, Error>{
    let client = OAuthClientBuilder::new(SPOTIFY_CLIENT_ID, SPOTIFY_REDIRECT_URI, SCOPES.to_vec())
        .build()
        .map_err(|e| Error::internal(format!("Unable to build OAuth client: {e}")))?;

    let token_response = client.get_access_token_async().await
        .map_err(|e| Error::unavailable(format!("Unable to get OAuth token: {e}")))?;

    println!("OAuth Token: {:#?}", token_response);

    // Refreshing token
    let refresh_token = token_response.refresh_token.clone();
    let refreshed = client.refresh_token_async(&refresh_token).await 
        .map_err(|e| Error::unknown(format!("Unable to refresh OAuth token: {e}")))?;

    println!("Refreshed OAuth Token: {:#?}", refreshed);

    Ok(token_response)
    
}

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
