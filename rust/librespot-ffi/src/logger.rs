use jni::objects::JObject;
use jni::JNIEnv;

/// Internal helper to call Log::<method>
fn log_internal(env: &mut JNIEnv, method: &str, tag: &str, msg: &str) {
    let jtag = env
        .new_string(tag)
        .expect("Couldn't create tag java string");
    let jmsg = env
        .new_string(msg)
        .expect("Couldn't create msg java string");

    let log_class = env
        .find_class("android/util/Log")
        .expect("Couldn't find android.util.Log");

    env.call_static_method(
        log_class,
        method,
        "(Ljava/lang/String;Ljava/lang/String;)I",
        &[(&jtag).into(), (&jmsg).into()],
    )
    .expect("android.util.Log call failed");
}

pub fn log_verbose(env: &mut JNIEnv, tag: &str, msg: &str) {
    log_internal(env, "v", tag, msg);
}

pub fn log_debug(env: &mut JNIEnv, tag: &str, msg: &str) {
    log_internal(env, "d", tag, msg);
}

pub fn log_info(env: &mut JNIEnv, tag: &str, msg: &str) {
    log_internal(env, "i", tag, msg);
}

pub fn log_warn(env: &mut JNIEnv, tag: &str, msg: &str) {
    log_internal(env, "w", tag, msg);
}

pub fn log_error(env: &mut JNIEnv, tag: &str, msg: &str) {
    log_internal(env, "e", tag, msg);
}

#[macro_export]
macro_rules! logv {
    ($env:expr, $tag:expr, $fmt:expr $(, $args:expr)* $(,)?) => {{
        let msg = format!($fmt $(, $args)*);
        $crate::logger::log_verbose(&mut $env, $tag, &msg);
    }};
    ($env:expr, $fmt:expr $(, $args:expr)* $(,)?) => {{
        let msg = format!($fmt $(, $args)*);
        $crate::logger::log_verbose(&mut $env, module_path!(), &msg);
    }};
}

#[macro_export]
macro_rules! logd {
    ($env:expr, $tag:expr, $fmt:expr $(, $args:expr)* $(,)?) => {{
        let msg = format!($fmt $(, $args)*);
        $crate::logger::log_debug(&mut $env, $tag, &msg);
    }};
    ($env:expr, $fmt:expr $(, $args:expr)* $(,)?) => {{
        let msg = format!($fmt $(, $args)*);
        $crate::logger::log_debug(&mut $env, module_path!(), &msg);
    }};
}

#[macro_export]
macro_rules! logi {
    ($env:expr, $tag:expr, $fmt:expr $(, $args:expr)* $(,)?) => {{
        let msg = format!($fmt $(, $args)*);
        $crate::logger::log_info(&mut $env, $tag, &msg);
    }};
    ($env:expr, $fmt:expr $(, $args:expr)* $(,)?) => {{
        let msg = format!($fmt $(, $args)*);
        $crate::logger::log_info(&mut $env, module_path!(), &msg);
    }};
}

#[macro_export]
macro_rules! logw {
    ($env:expr, $tag:expr, $fmt:expr $(, $args:expr)* $(,)?) => {{
        let msg = format!($fmt $(, $args)*);
        $crate::logger::log_warn(&mut $env, $tag, &msg);
    }};
    ($env:expr, $fmt:expr $(, $args:expr)* $(,)?) => {{
        let msg = format!($fmt $(, $args)*);
        $crate::logger::log_warn(&mut $env, module_path!(), &msg);
    }};
}

#[macro_export]
macro_rules! loge {
    ($env:expr, $tag:expr, $fmt:expr $(, $args:expr)* $(,)?) => {{
        let msg = format!($fmt $(, $args)*);
        $crate::logger::log_error(&mut $env, $tag, &msg);
    }};
    ($env:expr, $fmt:expr $(, $args:expr)* $(,)?) => {{
        let msg = format!($fmt $(, $args)*);
        $crate::logger::log_error(&mut $env, module_path!(), &msg);
    }};
}
