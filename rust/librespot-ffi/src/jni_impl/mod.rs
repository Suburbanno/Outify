// We seperate JNI only logic from the rust wrapper
use crate::spirc::SpircRuntime;
use once_cell::sync::OnceCell;

mod oauth;
pub mod playback;
mod spirc;
mod metadata;
mod spclient;

static SPIRC_RUNTIME: OnceCell<SpircRuntime> = OnceCell::new();
