use std::time::SystemTime;

use serde::{Deserialize, Serialize};

#[derive(Clone, Serialize, Deserialize)]
pub struct WebApiToken {
    pub access_token: String,
    pub refresh_token: String,
    pub expires_at: u64,
}

impl WebApiToken {
    pub fn new(access_token: String, refresh_token: String, expires_in: u64) -> Self {
        let now = SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .expect("Time went backwards")
            .as_secs();
        Self {
            access_token,
            refresh_token,
            expires_at: now + expires_in,
        }
    }

    pub fn is_expired(&self) -> bool {
        let now = SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .expect("Time went backwards")
            .as_secs();
        now >= self.expires_at
    }

    pub fn from(token: TokenResponse) -> Self {
        let now = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .expect("Time went backwards")
            .as_secs();

        Self {
            access_token: token.access_token,
            refresh_token: token.refresh_token,
            expires_at: now + token.expires_in
        }
    }
}

#[derive(Deserialize)]
pub(crate) struct TokenResponse {
    pub access_token: String,
    pub token_type: String,
    pub expires_in: u64,
    pub refresh_token: String,
}
