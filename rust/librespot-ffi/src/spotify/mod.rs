// Here we communicate with the official Spotify API

use oauth2::reqwest::Client;
use once_cell::sync::Lazy;

pub mod client;
pub mod token;
pub mod error;

mod search;
