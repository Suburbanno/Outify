use crate::{
    TOKIO_RUNTIME,
    metadata::user::UserJson,
    session::{SESSION, with_session},
};
use jni::{
    objects::{JClass, JString},
    sys::jstring,
};

pub async fn get_user_profile(username: Option<String>) -> Option<UserJson> {
    let session = match with_session(|s| s.clone()) {
        Ok(s) => s,
        Err(e) => {
            error!("Session unavailable: {}", e);
            return None;
        }
    };

    let username = match username {
        Some(u) => u,
        None => session.username(),
    };
    let limit: u32 = 5000;

    let result = match session
        .spclient()
        .get_user_profile(&username, Some(limit), Some(limit))
        .await
    {
        Ok(r) => r,
        Err(e) => {
            error!("failed to get user profile: {e}");
            return None;
        },
    };

    let json = match String::from_utf8(result.to_vec()) {
        Ok(j) => j,
        Err(e) => {
            error!("failed to get string from bytes: {e}");
            return None;
        },
    };

    let profile: UserJson = match serde_json::from_str(&json) {
        Ok(p) => p,
        Err(e) => {
            error!("failed to deserialize UserJson: {e}");
            return None;
        }
    };

    Some(profile)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_UserProfile_getUserProfile(
    mut env: jni::JNIEnv,
    _this: JClass,
    username: JString,
) -> jstring {
    let rt = match TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            warn!("Failed to initialize session as Tokio Runtime is not initialized!");
            return std::ptr::null_mut();
        }
    };

    let username: Option<String> = if username.is_null() {
        None
    } else {
        match env.get_string(&username) {
            Ok(js) => Some(js.into()),
            Err(e) => {
                error!("failed to get username: {}", e);
                return std::ptr::null_mut();
            }
        }
    };

    let profile: UserJson = match rt.block_on(get_user_profile(username)) {
        Some(u) => u,
        None => {
            log::error!("failed to get user profile");
            return std::ptr::null_mut();
        }
    };

    let json = match serde_json::to_string(&profile) {
        Ok(j) => j,
        Err(e) => {
            log::error!("failed to serialize UserJson: {}", e);
            return std::ptr::null_mut();
        }
    };

    // Convert Rust String -> jstring and return
    match env.new_string(json) {
        Ok(j) => j.into_raw(),
        Err(e) => {
            log::error!("failed to convert json to JNI string: {}", e);
            std::ptr::null_mut()
        }
    }
}
