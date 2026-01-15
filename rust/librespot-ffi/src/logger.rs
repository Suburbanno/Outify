use jni::{JNIEnv, JavaVM};
use log::{Level, LevelFilter, Metadata, Record};
use std::ffi::CString;

const ANDROID_LOG_VERBOSE: i32 = 2;
const ANDROID_LOG_DEBUG: i32 = 3;
const ANDROID_LOG_INFO: i32 = 4;
const ANDROID_LOG_WARN: i32 = 5;
const ANDROID_LOG_ERROR: i32 = 6;

unsafe extern "C" {
    fn __android_log_write(prio: i32, tag: *const i8, text: *const i8) -> i32;
}

pub struct AndroidLogger {
    #[allow(dead_code)]
    jvm: JavaVM,
}

impl AndroidLogger {
    /// Initialize the global logger.
    pub fn init(jvm: JavaVM, level: LevelFilter) -> Result<(), log::SetLoggerError> {
        let logger = AndroidLogger { jvm };
        log::set_boxed_logger(Box::new(logger))?;
        log::set_max_level(level);
        Ok(())
    }
}

fn log_to_ndk(level: Level, tag: &str, msg: &str) {
    let tag = if tag.is_empty() { "rust" } else { tag };
    let tag_c = CString::new(tag).unwrap_or_else(|_| CString::new("rust").unwrap());
    // if message contains NULs, fall back to a safe placeholder
    let msg_c = CString::new(msg).unwrap_or_else(|_| CString::new("invalid log message").unwrap());

    let prio = match level {
        Level::Error => ANDROID_LOG_ERROR,
        Level::Warn => ANDROID_LOG_WARN,
        Level::Info => ANDROID_LOG_INFO,
        Level::Debug => ANDROID_LOG_DEBUG,
        Level::Trace => ANDROID_LOG_VERBOSE,
    };

    unsafe {
        let _ = __android_log_write(prio, tag_c.as_ptr() as *const i8, msg_c.as_ptr() as *const i8);
    }
}

impl log::Log for AndroidLogger {
    fn enabled(&self, _metadata: &Metadata) -> bool {
        true
    }

    fn log(&self, record: &Record) {
        if !self.enabled(record.metadata()) {
            return;
        }

        let tag = record.target();
        let msg = format!("{}", record.args());
        log_to_ndk(record.level(), tag, &msg);
    }

    fn flush(&self) {}
}

/// Legacy-compatible functions that accept a JNIEnv (but *do not* use it).
/// This preserves your existing call sites/macros that pass `env`.
pub fn log_verbose(tag: &str, msg: &str) {
    log_to_ndk(Level::Trace, tag, msg);
}

pub fn log_debug(tag: &str, msg: &str) {
    log_to_ndk(Level::Debug, tag, msg);
}

pub fn log_info(tag: &str, msg: &str) {
    log_to_ndk(Level::Info, tag, msg);
}

pub fn log_warn(tag: &str, msg: &str) {
    log_to_ndk(Level::Warn, tag, msg);
}

pub fn log_error(tag: &str, msg: &str) {
    log_to_ndk(Level::Error, tag, msg);
}

/// Macros preserved from your original code so call-sites don't need to change.
/// They still accept an env argument but it's ignored by the implementation above.
#[macro_export]
macro_rules! logv {
    ($env:expr, $tag:expr, $fmt:expr $(, $args:expr)* $(,)?) => {{
        let msg = format!($fmt $(, $args)*);
        $crate::logger::log_verbose($tag, &msg);
    }};
    ($env:expr, $fmt:expr $(, $args:expr)* $(,)?) => {{
        let msg = format!($fmt $(, $args)*);
        $crate::logger::log_verbose(module_path!(), &msg);
    }};
}

#[macro_export]
macro_rules! logd {
    ($env:expr, $tag:expr, $fmt:expr $(, $args:expr)* $(,)?) => {{
        let msg = format!($fmt $(, $args)*);
        $crate::logger::log_debug($tag, &msg);
    }};
    ($env:expr, $fmt:expr $(, $args:expr)* $(,)?) => {{
        let msg = format!($fmt $(, $args)*);
        $crate::logger::log_debug(module_path!(), &msg);
    }};
}

#[macro_export]
macro_rules! logi {
    ($env:expr, $tag:expr, $fmt:expr $(, $args:expr)* $(,)?) => {{
        let msg = format!($fmt $(, $args)*);
        $crate::logger::log_info($tag, &msg);
    }};
    ($env:expr, $fmt:expr $(, $args:expr)* $(,)?) => {{
        let msg = format!($fmt $(, $args)*);
        $crate::logger::log_info(module_path!(), &msg);
    }};
}

#[macro_export]
macro_rules! logw {
    ($env:expr, $tag:expr, $fmt:expr $(, $args:expr)* $(,)?) => {{
        let msg = format!($fmt $(, $args)*);
        $crate::logger::log_warn($tag, &msg);
    }};
    ($env:expr, $fmt:expr $(, $args:expr)* $(,)?) => {{
        let msg = format!($fmt $(, $args)*);
        $crate::logger::log_warn(module_path!(), &msg);
    }};
}

#[macro_export]
macro_rules! loge {
    ($env:expr, $tag:expr, $fmt:expr $(, $args:expr)* $(,)?) => {{
        let msg = format!($fmt $(, $args)*);
        $crate::logger::log_error($tag, &msg);
    }};
    ($env:expr, $fmt:expr $(, $args:expr)* $(,)?) => {{
        let msg = format!($fmt $(, $args)*);
        $crate::logger::log_error(module_path!(), &msg);
    }};
}

