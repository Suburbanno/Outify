use crate::{TOKIO_RUNTIME, session::SESSION};
use jni::{objects::JClass, sys::JNIEnv};
use librespot_core::Session;

pub async fn get_user_profile() {
    info!("Getting user profile..");
    let session_ref = match SESSION.get() {
        Some(sess) => sess,
        None => {
            error!("Cannot get user profile as session is undefined!");
            return;
        }
    };

    let username = session_ref.username();
    let limit: u32 = 5000;

    info!("Requesting user profile");
    let result = session_ref
        .spclient()
        .get_user_profile(&username, Some(limit), Some(limit))
        .await
        .unwrap();
    info!("User profile: {}", String::from_utf8(result.to_vec()).unwrap());
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_profile_UserProfile_getUserProfile(
    env: JNIEnv,
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
