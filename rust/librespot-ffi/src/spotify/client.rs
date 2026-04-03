use librespot_core::{authentication::Credentials, token::Token};
use librespot_oauth::{OAuthClient, OAuthClientBuilder, OAuthToken};
use oauth2::{AuthorizationCode, PkceCodeVerifier, url::Url};
use once_cell::sync::{Lazy, OnceCell};
use reqwest::{Client, StatusCode, header};
use serde::{Deserialize, Serialize};
use serde_json::{Value, json};
use std::{
    fs::OpenOptions,
    os::unix::fs::OpenOptionsExt,
    path::Path,
    sync::Arc,
    time::{Duration, Instant, SystemTime, UNIX_EPOCH},
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
const SPOTIFY_OAUTH_CALLBACK_URI: &str = "http://127.0.0.1:5588/account/login";
const SPOTIFY_OAUTH_SCOPES: &[&str] = &[
    "streaming",
    "user-read-private",
    "user-read-email",
    "user-library-modify",
    "user-library-read",
];

static SPOTIFY_CLIENT: OnceCell<SpotifyClient> = OnceCell::new();

/// OAuth state for SpotifyClient's user authentication flow
pub struct OAuthState {
    pub oauth_client: OAuthClient,
    pub pkce_verifier: Option<PkceCodeVerifier>,
    pub created_at: Instant,
}

#[derive(Clone)]
pub struct SpotifyClient {
    client_id: String,
    client_secret: String,
    client: Client,
    token: Arc<RwLock<Option<WebApiToken>>>,
    oauth_state: Arc<RwLock<Option<OAuthState>>>,
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
            oauth_state: Arc::new(RwLock::new(None)),
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
        let token = match self.load_token().await {
            Ok(o) => {
                match o {
                    Some(t) => t,
                    None => {
                        return Err(SpotifyApiError::Generic("No account token present!".to_string()));
                    },
                }
            },
            Err(e) => {
                return Err(e);
            },
        };

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
                    }
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

        let now = std::time::SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .expect("Time went backwards")
            .as_secs();

        let new_token = WebApiToken::new(res.access_token.clone(), now + res.expires_in);

        let mut write_guard = self.token.write().await;
        *write_guard = Some(new_token);

        Some(res.access_token)
    }

    pub async fn get_oauth_url(&self) -> String {
        format!("{}", SPOTIFY_OAUTH_CALLBACK_URI)
    }

    /// Starts the OAuth flow and returns the authorization URL
    pub async fn start_oauth_flow(&self) -> Result<String, SpotifyApiError> {
        // Build OAuthClient using librespot_oauth
        let oauth_client = OAuthClientBuilder::new(
            &self.client_id,
            SPOTIFY_OAUTH_CALLBACK_URI,
            SPOTIFY_OAUTH_SCOPES.to_vec(),
        )
        .build()
        .map_err(|e| SpotifyApiError::Generic(format!("Failed to build OAuth client: {}", e)))?;

        // Get authorization URL and PKCE verifier
        let (auth_url, pkce_verifier) = oauth_client.set_auth_url();

        let state = OAuthState {
            oauth_client,
            pkce_verifier: Some(pkce_verifier),
            created_at: Instant::now(),
        };

        let mut oauth_state_guard = self.oauth_state.write().await;
        *oauth_state_guard = Some(state);

        debug!("OAuth flow started with URL: {}", auth_url);
        Ok(auth_url.to_string())
    }

    /// Completes the OAuth flow by exchanging the authorization code for tokens
    pub async fn complete_oauth_flow(&self, code: String) -> Result<WebApiToken, SpotifyApiError> {
        let mut oauth_state_guard = self.oauth_state.write().await;
        let state = oauth_state_guard.as_mut().ok_or(SpotifyApiError::Generic(
            "OAuth flow not started. Call start_oauth_flow first.".to_string(),
        ))?;

        // Check if state is still valid (not expired)
        if state.created_at.elapsed() > Duration::from_secs(600) {
            error!("OAuth state expired");
            return Err(SpotifyApiError::Generic(
                "OAuth state expired. Please restart the flow.".to_string(),
            ));
        }

        let pkce_verifier = state.pkce_verifier.take().ok_or(SpotifyApiError::Generic(
            "PKCE verifier not found. OAuth flow may have already completed.".to_string(),
        ))?;
        let oauth_client = &state.oauth_client;

        // Use the OAuthClient to exchange code for token
        let auth_code = AuthorizationCode::new(code);
        let token_response: OAuthToken = oauth_client
            .get_access_token_with_verifier_async(pkce_verifier, auth_code)
            .await
            .map_err(|e| {
                error!("OAuth token exchange failed: {}", e);
                SpotifyApiError::Generic(format!("Token exchange failed: {}", e))
            })?;

        // Calculate remaining seconds until expiration
        let now = Instant::now();
        let expires_in = if token_response.expires_at > now {
            token_response.expires_at.duration_since(now).as_secs()
        } else {
            0
        };

        let new_token = WebApiToken::new(token_response.access_token, expires_in);

        info!("token: {}", new_token.access_token);

        let mut token_guard = self.token.write().await;
        *token_guard = Some(new_token.clone());

        // Clear OAuth state after successful exchange
        drop(oauth_state_guard);
        let mut oauth_state_guard = self.oauth_state.write().await;
        *oauth_state_guard = None;

        debug!("OAuth flow completed successfully");

        // Save token to account.json
        match self.save_token(&new_token).await {
            Ok(_) => debug!("Account token saved!"),
            Err(e) => {
                error!("Failed to save account token: {e}");
            }
        };

        Ok(new_token)
    }

    pub async fn save_token(&self, token: &WebApiToken) -> Result<(), SpotifyApiError> {
        let mut path: &mut std::path::PathBuf = match crate::FILES_DIR.get() {
            Some(p) => &mut p.clone(),
            None => {
                error!("Android file path is not set!");
                return Err(SpotifyApiError::Generic(
                    "Android file path is not set!".to_string(),
                ));
            }
        };
        path.push("account.json");

        let mut file = OpenOptions::new()
            .write(true)
            .create(true)
            .truncate(true)
            .mode(0o600)
            .open(path)?;

        let json = match serde_json::to_string(token) {
            Ok(j) => j,
            Err(e) => {
                return Err(SpotifyApiError::Generic(
                    "Failed to serialize WebApiToken: {e}".to_string(),
                ));
            }
        };

        std::io::Write::write_all(&mut file, json.as_bytes())?;
        Ok(())
    }

    /// Loads the token from the session's cache if available
    pub async fn load_token(&self) -> Result<Option<WebApiToken>, SpotifyApiError> {
        let mut path: &mut std::path::PathBuf = match crate::FILES_DIR.get() {
            Some(p) => &mut p.clone(),
            None => {
                error!("Android file path is not set!");
                return Err(SpotifyApiError::Generic(
                    "Android file path is not set!".to_string(),
                ));
            }
        };
        path.push("account.json");

        match std::fs::read_to_string(path) {
            Ok(contents) => {
                let token: WebApiToken = serde_json::from_str(&contents)?;
                Ok(Some(token))
            }
            Err(e) if e.kind() == std::io::ErrorKind::NotFound => Ok(None),
            Err(e) => Err(SpotifyApiError::IO(e)),
        }
    }
}

pub fn init_client(client_id: String, client_secret: String) {
    let client = SpotifyClient::new(client_id, client_secret);
    SPOTIFY_CLIENT.set(client);
}

pub fn get_client() -> &'static SpotifyClient {
    SPOTIFY_CLIENT
        .get()
        .expect("SpotifyClient not initialized!")
}
