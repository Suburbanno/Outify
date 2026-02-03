use librespot_core::{Error, Session};
use librespot_oauth::{OAuthClient, OAuthClientBuilder, OAuthToken};
use oauth2::{AuthorizationCode, PkceCodeVerifier, url::Url};
use once_cell::sync::OnceCell;
use tokio::sync::Mutex;

pub const SPOTIFY_CALLBACK_URI: &str = "http://127.0.0.1:5588/login";
static OAUTH_SCOPES: &[&str] = &[
    "app-remote-control",
    "playlist-modify",
    "playlist-modify-private",
    "playlist-modify-public",
    "playlist-read",
    "playlist-read-collaborative",
    "playlist-read-private",
    "streaming",
    "ugc-image-upload",
    "user-follow-modify",
    "user-follow-read",
    "user-library-modify",
    "user-library-read",
    "user-modify",
    "user-modify-playback-state",
    "user-modify-private",
    "user-personalized",
    "user-read-birthdate",
    "user-read-currently-playing",
    "user-read-email",
    "user-read-play-history",
    "user-read-playback-position",
    "user-read-playback-state",
    "user-read-private",
    "user-read-recently-played",
    "user-top-read",
];


pub static OAUTH_SESSION: OnceCell<Mutex<OAuthSession>> = OnceCell::new();

pub struct OAuthSession {
    client: OAuthClient,
    pub(crate) session: Session,
    pub pkce_verifier: Option<PkceCodeVerifier>,
    pub auth_url: Url,
}

impl OAuthSession {
    pub fn new(session: &Session, redirect_uri: &str, scopes: &[&str]) -> Result<Self, Error> {
        let client_id = session.client_id();
        let client = OAuthClientBuilder::new(client_id.as_str(), redirect_uri, scopes.to_vec())
            .build()
            .map_err(|e| Error::internal(format!("Unable to build OAuth client: {e}")))?;
        let (auth_url, pkce_verifier) = client.set_auth_url();

        Ok(Self {
            client,
            session: session.clone(),
            pkce_verifier: Some(pkce_verifier),
            auth_url,
        })
    }

    pub fn auth_url(&self) -> &Url {
        &self.auth_url
    }

    /// Retrieves the access blob using the OAuth code.
    pub async fn get_access_token(&mut self, code: String) -> Result<OAuthToken, Error> {
        let pkce_verifier = self
            .pkce_verifier
            .take()
            .ok_or_else(|| Error::internal("Missing Pkce Verifier".to_string()))?;

        let auth_code = AuthorizationCode::new(code);

        let token_response = self
            .client
            .get_access_token_with_verifier_async(pkce_verifier, auth_code)
            .await
            .map_err(|e| Error::unavailable(format!("Unable to get OAuth token: {e}")))?;

        // Refreshing token to provide consistent TokenResponse that contains refresh token
        let refresh_token = token_response.refresh_token.clone();
        let _refreshed = self
            .client
            .refresh_token_async(&refresh_token)
            .await
            .map_err(|e| Error::unknown(format!("Unable to refresh OAuth token: {e}")))?;

        Ok(token_response)
    }
}

fn oauth_session_cell() -> &'static Mutex<OAuthSession> {
    OAUTH_SESSION.get_or_init(|| {
        // note: we intentionally create an empty placeholder. The real session is set up later in `setup_oauth_session`.
        Mutex::new(unsafe { std::mem::MaybeUninit::zeroed().assume_init() })
    })
}

pub fn setup_oauth_session(session: &Session) -> Option<&'static Mutex<OAuthSession>> {
    debug!("Setting up OAuthSession");
    if let Some(existing) = OAUTH_SESSION.get() {
        debug!("OAuthSession already initialized!");
        return Some(existing);
    }

    let osession = match OAuthSession::new(&session, &SPOTIFY_CALLBACK_URI, OAUTH_SCOPES) {
        Ok(s) => s,
        Err(e) => {
            error!("OAuth session setup failed with: {}", e);
            return None;
        }
    };

    if OAUTH_SESSION.set(Mutex::new(osession)).is_err() {
        warn!("Failed to set OAuth Session concurrently!");
    }

    OAUTH_SESSION.get()
}
