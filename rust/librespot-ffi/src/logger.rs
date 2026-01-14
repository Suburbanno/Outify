use jni::objects::JObject;
use jni::{JNIEnv, JavaVM};

use log::{Level, LevelFilter, Metadata, Record};

pub struct AndroidLogger {
    jvm: JavaVM,
}

impl AndroidLogger {
    pub fn init(jvm: JavaVM, level: LevelFilter) -> Result<(), log::SetLoggerError> {
        let logger = AndroidLogger { jvm };
        log::set_boxed_logger(Box::new(logger))?;
        log::set_max_level(level);
        Ok(())
    }

    fn log_to_android(env: &mut JNIEnv, level: log::Level, target: &str, msg: &str) {
        let tag = target;
        match level {
            log::Level::Error => crate::logger::log_error(env, tag, msg),
            log::Level::Warn => crate::logger::log_warn(env, tag, msg),
            log::Level::Info => crate::logger::log_info(env, tag, msg),
            log::Level::Debug => crate::logger::log_debug(env, tag, msg),
            log::Level::Trace => crate::logger::log_verbose(env, tag, msg),
        }
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

        if let Ok(mut env) = self.jvm.attach_current_thread_as_daemon() {
            Self::log_to_android(
                &mut env,
                record.level(),
                record.target(),
                &format!("{}", record.args()),
            );
        }
    }
    fn flush(&self) {}
}

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
