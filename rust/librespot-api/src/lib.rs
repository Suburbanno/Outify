#![crate_name = "librespot_api"]

mod errors;
pub mod oauth;

pub use errors::Error;

use std::env;

use librespot_core::{
    config::SessionConfig, session::Session, token::Token,
};
use librespot_oauth::{OAuthClientBuilder, OAuthToken};
use oauth2::{url::Url};

// Exposing required librespot structs
pub use librespot_core::{
    authentication::Credentials
};
pub use librespot_playback::audio_backend::android::{AndroidSink,PcmCallback};
pub use librespot_playback::config::AudioFormat;
