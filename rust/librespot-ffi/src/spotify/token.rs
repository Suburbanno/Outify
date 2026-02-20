use std::{sync::RwLock, time::Instant};

use oauth2::reqwest::Client;
use once_cell::sync::Lazy;
use serde::Deserialize;

#[derive(Clone)]
pub struct WebApiToken {
    pub access_token: String,
    pub expires_at: Instant,
}

impl WebApiToken {
    pub fn is_expired(&self) -> bool {
        Instant::now() >= self.expires_at
    }
}

#[derive(Deserialize)]
pub(crate) struct TokenResponse {
    pub access_token: String,
    pub token_type: String,
    pub expires_in: u64,
}
