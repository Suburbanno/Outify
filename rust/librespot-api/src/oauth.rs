use librespot_oauth::{OAuthClient, OAuthClientBuilder, OAuthToken};
use oauth2::{PkceCodeVerifier, url::Url, AuthorizationCode};

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
        let pkce_verifier = self.pkce_verifier
            .take()
            .ok_or(Error::internal(format!("Missing Pkce Verifier")))?;

        let auth_code = AuthorizationCode::new(code);

        let token_response = self
            .client
            .get_access_token_with_verifier_async(pkce_verifier, auth_code)
            .await
            .map_err(|e| Error::unavailable(format!("Unable to get OAuth token: {e}")))?;

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
}
