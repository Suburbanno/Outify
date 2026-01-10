#[derive(Debug)]
#[uniffi::export] 
pub enum SpotifyError {
    InvalidCredentials,
    NetworkError,
    UnknownError,
}

impl From<librespot::Error> for SpotifyError {
    fn from(err: librespot::Error) -> Self {
        use librespot::Error::*;
        match err {
            InvalidCredentials => SpotifyError::InvalidCredentials,
            NetworkError(_) => SpotifyError::NetworkError,
            _ => SpotifyError::UnknownError,
        }
    }
}

