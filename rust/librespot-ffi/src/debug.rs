// Used for testing work in progress features
use crate::{ANDROID_CLIENT_ID, OUTIFY_CLIENT_ID, TOKIO_RUNTIME};
use jni::{objects::JClass, sys::JNIEnv};

#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_Debug_debug1(_env: JNIEnv, _this: JClass) {
    let rt = match TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            warn!("Failed to initialize session as Tokio Runtime is not initialized!");
            return;
        }
    };

    info!("Attempting login5");
    let session = crate::session::SESSION.get().unwrap();
    rt.block_on(async {
        match session
            .login5()
            .login(
            )
            .await
        {
            Ok(token) => {
                info!("Login5 Success: ");
            }
            Err(e) => {
                warn!("Login5 err: {e}");
            }
        }
        info!("Attempted login5");
    });
}
