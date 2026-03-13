use librespot_core::token::Token;
use once_cell::sync::{Lazy, OnceCell};
use reqwest::{Client, StatusCode, header};
use serde::Deserialize;
use serde_json::Value;
use std::{
    sync::Arc,
    time::{Duration, Instant},
};
use tokio::sync::RwLock;

use crate::{
    session::with_session,
    spotify::{
        error::SpotifyApiError,
        search::extract_all_uris,
        token::{TokenResponse, WebApiToken},
    },
};

const SPOTIFY_API_URL: &str = "https://api.spotify.com";
const REQUEST_TIMEOUT: Duration = Duration::from_secs(5);

static SPOTIFY_CLIENT: OnceCell<SpotifyClient> = OnceCell::new();

#[derive(Clone)]
pub struct SpotifyClient {
    client_id: String,
    client_secret: String,
    client: Client,
    token: Arc<RwLock<Option<WebApiToken>>>,
}

impl SpotifyClient {
    pub fn new(client_id: String, client_secret: String) -> Self {
        Self {
            client_id,
            client_secret,
            client: Client::builder()
                .pool_idle_timeout(Duration::from_secs(90))
                .build()
                .expect("failed to build client"),
            token: Arc::new(RwLock::new(None)),
        }
    }

    pub async fn search(
        &self,
        query: &str,
        types: &str,
        limit: Option<i32>,
        offset: Option<i32>,
    ) -> Result<Vec<String>, SpotifyApiError> {
        let token = self.get_token().await.ok_or(SpotifyApiError::NoToken)?;

        let mut params = vec![("q", query.to_string()), ("type", types.to_string())];

        if let Some(l) = limit {
            params.push(("limit", l.to_string()));
        }

        if let Some(o) = offset {
            params.push(("offset", o.to_string()));
        }

        let res = self
            .client
            .get(format!("{}/v1/search", SPOTIFY_API_URL))
            .query(&params)
            .bearer_auth(token)
            .timeout(REQUEST_TIMEOUT)
            .send()
            .await?;

        info!("STATUS: {}", &res.status().as_str());

        let text = res.text().await?;
        info!("BODY SIZE: {}", text.len());

        let parsed: crate::spotify::search::SearchResponse = serde_json::from_str(&text)?;

        let uris = extract_all_uris(parsed);

        Ok(uris)
    }

    // Saves tracks/episodes/albums/..
    pub async fn save_items(&self, uris: Vec<String>) -> Result<StatusCode, SpotifyApiError> {
        // let token = self.get_token().await.ok_or(SpotifyApiError::NoToken)?;
        let token: Token = crate::session::with_session_async(|ses| {
            Box::pin(async move {
                // Attempt to get a token
                match ses.login5().auth_token().await {
                    Ok(t) => Ok(t),
                    Err(e) => {
                        // Log the error for debugging
                        log::error!("Failed to get Spotify token: {:?}", e);
                        // Convert to your error type
                        Err(SpotifyApiError::NoToken)
                    }
                }
            })
        })
        .await??;

        info!("token: {}", &token.access_token);

        let ids = uris.join(",");

        let res = self
            .client
            .put(format!("{}/v1/me/library", SPOTIFY_API_URL))
            .query(&[("uris", ids)])
            .header(header::CONTENT_LENGTH, "0")
            .bearer_auth(token.access_token)
            .timeout(REQUEST_TIMEOUT)
            .send()
            .await?;

        // Debug purposes
        if res.status() == reqwest::StatusCode::TOO_MANY_REQUESTS {
            if let Some(retry_after) = res.headers().get("Retry-After") {
                let seconds = match retry_after.to_str() {
                    Ok(s) => {
                        debug!("wait: {s}");
                    }
                    Err(e) => {
                        debug!("failed: {e}");
                    },
                };
            }
        }

        Ok(res.status())
    }

    pub async fn get_token(&self) -> Option<String> {
        {
            // Reading existing token
            let read_guard = self.token.read().await;
            if let Some(token) = &*read_guard {
                if !token.is_expired() {
                    return Some(token.access_token.clone());
                }
            }
        }

        let res = self
            .client
            .post("https://accounts.spotify.com/api/token")
            .basic_auth(&self.client_id, Some(&self.client_secret))
            .form(&[("grant_type", "client_credentials")])
            .timeout(REQUEST_TIMEOUT)
            .send()
            .await
            .ok()?
            .json::<TokenResponse>()
            .await
            .ok()?;

        let new_token = WebApiToken {
            access_token: res.access_token.clone(),
            expires_at: Instant::now() + Duration::from_secs(res.expires_in),
        };

        let mut write_guard = self.token.write().await;
        *write_guard = Some(new_token);

        Some(res.access_token)
    }
}

pub fn init_client() {
    let client = SpotifyClient::new(
        "819a62c83de24821b2654387bc84f136".to_string(),
        "6db424c706d34cf7810a5c8c59324182".to_string(),
    );
    SPOTIFY_CLIENT.set(client);
}

pub fn get_client() -> &'static SpotifyClient {
    SPOTIFY_CLIENT
        .get()
        .expect("SpotifyClient not initialized!")
}
