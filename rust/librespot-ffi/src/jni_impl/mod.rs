// We seperate JNI only logic from the rust wrapper
use crate::spirc::SpircRuntime;
use once_cell::sync::OnceCell;

mod spirc;
mod oauth;

static SPIRC_RUNTIME: OnceCell<SpircRuntime> = OnceCell::new();
