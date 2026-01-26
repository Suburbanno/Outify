use jni::{objects::JClass, sys::jboolean, JNIEnv};

// Checks for cached credentials. Does NOT use Session
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_SpAuthManager_hasCachedCredentials(
    env: JNIEnv,
    _this: JClass,
) -> jboolean {
    let dir = match crate::FILES_DIR.get() {
        Some(d) => d,
        None => {
            warn!("Cannot check for cached credentials as FILES_DIR is not set.");
            return 0;
        }
    };

    dir.join("credentials.json").is_file() as jboolean
}
