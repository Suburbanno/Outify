use librespot_oauth::{OAuthClient, OAuthClientBuilder, OAuthToken};
use oauth2::{AuthorizationCode, PkceCodeVerifier, url::Url};

use super::errors::Error;

pub struct OAuthSession {
    client: OAuthClient,
    pub pkce_verifier: Option<PkceCodeVerifier>,
    pub auth_url: Url,
}

impl OAuthSession {
    pub fn new(client_id: &str, redirect_uri: &str, scopes: &[&str]) -> Result<Self, Error> {
        let client = OAuthClientBuilder::new(client_id, redirect_uri, scopes.to_vec())
            .build()
            .map_err(|e| Error::internal(format!("Unable to build OAuth client: {e}")))?;
        let (auth_url, pkce_verifier) = client.set_auth_url();

        Ok(Self {
            client,
            pkce_verifier: Some(pkce_verifier),
            auth_url,
        })
    }

    pub fn auth_url(&self) -> &Url {
        &self.auth_url
    }

    /// Retrieves the Access Token.
    /// Automatically refreshes it.
    pub async fn get_access_token(&mut self, code: String) -> Result<OAuthToken, Error> {
        let pkce_verifier = self
            .pkce_verifier
            .take()
            .ok_or(Error::internal(format!("Missing Pkce Verifier")))?;

        log::info!(
            "get_access_token: pkce_verifier: {}",
            pkce_verifier.secret()
        );

        let auth_code = AuthorizationCode::new(code);
        log::info!("get_access_token: auth_code: {}", auth_code.secret());

        let token_response = self
            .client
            .get_access_token_with_verifier_async(pkce_verifier, auth_code)
            .await
            .map_err(|e| Error::unavailable(format!("Unable to get OAuth token: {e}")))?;

        log::info!("get_access_token: token_response: {:#?}", token_response);
        println!("OAuth Token: {:#?}", token_response);

        // Refreshing token
        let refresh_token = token_response.refresh_token.clone();
        let refreshed = self
            .client
            .refresh_token_async(&refresh_token)
            .await
            .map_err(|e| Error::unknown(format!("Unable to refresh OAuth token: {e}")))?;

        println!("Refreshed OAuth Token: {:#?}", refreshed);

        Ok(token_response)
    }

    /// Refreshes the auth token and retrieves a new one
    pub async fn refresh_token(&mut self, refresh_token: String) -> Result<String, Error> {
        let refreshed = self.
            client
            .refresh_token_async(&refresh_token)
            .await
            .map_err(|e| Error::unauthenticated(format!("Unable to refresh OAuth token: {e}")))?;

        log::debug!("Refreshed OAuth token!");

        OK(refreshed.refresh_token)
    }
}
