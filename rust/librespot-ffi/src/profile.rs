use crate::{
    TOKIO_RUNTIME,
    session::{SESSION, with_session},
};
use jni::{objects::JClass, sys::JNIEnv};

pub async fn get_user_profile() {
    info!("Getting user profile..");
    let session = match with_session(|s| s.clone()) {
        Ok(s) => s,
        Err(e) => {
            error!("Session unavailable: {}", e);
            return;
        }
    };

    let username = session.username();
    let limit: u32 = 5000;

    info!("Requesting user profile");
    let result = session
        .spclient()
        .get_user_profile(&username, Some(limit), Some(limit))
        .await
        .unwrap();
    info!(
        "User profile: {}",
        String::from_utf8(result.to_vec()).unwrap()
    );
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_profile_UserProfile_getUserProfile(
    _env: JNIEnv,
    _this: JClass,
) {
    info!("GetUserProfile");
    let rt = match TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            warn!("Failed to initialize session as Tokio Runtime is not initialized!");
            return;
        }
    };

    rt.block_on(async {
        get_user_profile().await;
    });
}
