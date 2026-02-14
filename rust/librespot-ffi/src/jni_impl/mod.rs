use std::sync::RwLock;

// We seperate JNI only logic from the rust wrapper
use crate::spirc::SpircRuntime;
use once_cell::sync::OnceCell;

mod metadata;
mod oauth;
pub mod playback;
mod session;
mod spclient;
mod spirc;
