//! Parts of code borrowed from
//! https://github.com/GreptimeTeam/rust-java-demo/blob/main/core/src/main/rust/demo/src/lib.rs
pub mod logger;
mod method_invoker;
pub mod oauth;

use librespot_api as api;

use api::{Error, oauth::OAuthSession};

use std::cell::RefCell;
use std::sync::{Mutex};

use once_cell::sync::OnceCell;

use jni::objects::{AutoLocal, JClass, JObject, JString, JThrowable, JValue};
use jni::sys::{jboolean, jstring};
use jni::{JNIEnv, JNIVersion, JavaVM, sys};

use tokio::runtime::Runtime;

use crate::logger::{CallState, Logger, info};

static INIT_LOCK: Mutex<bool> = Mutex::new(false);

const JNI_VERSION: JNIVersion = jni::JNIVersion::V8;

const LOGGER: Logger = Logger;
static GLOBAL_LOGGER: OnceCell<Logger> = OnceCell::new();

static RUNTIME: OnceCell<Runtime> = OnceCell::new();
static JAVA_VM: OnceCell<JavaVM> = OnceCell::new();
static CALL_STATE: OnceCell<CallState> = OnceCell::new();

thread_local! {
    static ENV: RefCell<Option<*mut jni::sys::JNIEnv>> = const { RefCell::new(None) };
}

fn runtime() -> &'static Runtime {
    RUNTIME
        .get()
        .expect("Runtime should have been initialized by calling the `libInit` first!")
}

fn java_vm() -> &'static JavaVM {
    JAVA_VM
        .get()
        .expect("JavaVM should have been initialized by calling the `libInit` first!")
}

fn call_state() -> &'static CallState {
    CALL_STATE
        .get()
        .expect("CallState should have been initialized by calling the `libInit` first!")
}

fn jni_env<'a>() -> JNIEnv<'a> {
    let env = ENV
        .with(|cell| *cell.borrow_mut())
        .expect("Not calling from inside tokio rt?");
    unsafe {
        JNIEnv::from_raw(env).unwrap_or_else(|e| panic!("Invalid 'JNIEnv' pointer? err: {:?}", e))
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_LibrespotFfi_libInit(
    mut env: JNIEnv,
    _class: JClass,
    runtime_size: sys::jint,
) {
    let mut init = INIT_LOCK.lock().unwrap();
    if *init {
        return;
    }

    if runtime_size < 0 {
        throw_runtime_exception(&mut env, "`runtime_size` cannot be less than 0".to_string());
        return;
    }

    let runtime_size = if runtime_size == 0 {
        num_cpus::get()
    } else {
        runtime_size as usize
    };

    let java_vm = JAVA_VM.get_or_try_init(|| env.get_java_vm());
    let java_vm = unwrap_or_throw!(&mut env, java_vm);

    let rt = RUNTIME.get_or_try_init(|| {
        tokio::runtime::Builder::new_multi_thread()
            .worker_threads(runtime_size)
            .on_thread_start(move || {
                ENV.with(|cell| {
                    let env =
                        unsafe { java_vm.attach_current_thread_as_daemon() }.unwrap_or_else(|e| {
                            panic!("Failed to attach tokio's threads to JVM, err: {e:?}'")
                        });
                    *cell.borrow_mut() = Some(env.get_raw());
                })
            })
            .enable_all()
            .build()
    });

    unwrap_or_throw!(&mut env, rt);

    let call_state = CALL_STATE.get_or_try_init(|| CallState::try_new(&mut env));
    unwrap_or_throw!(&mut env, call_state);

    GLOBAL_LOGGER.get_or_init(|| {
        log::set_logger(&LOGGER).expect("unable to set `Logger` as the global logger");
        log::set_max_level(log::LevelFilter::Trace);
        LOGGER
    });

    *init = true;

    info!(
        "Outify's LibrespotFfi lib is initialized with runtime size {}",
        runtime_size
    );
}

// LibrespotFfi isConnected
// Used for checking, whether the Rust <> JNI connection works
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_LibrespotFfi_isConnected(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    1
}

fn throw_runtime_exception(env: &mut JNIEnv, msg: String) {
    let msg = match env.exception_occurred() {
        Ok(ex) => {
            // Clear the Java exception so we can call JNI methods safely
            env.exception_clear();

            // Obtain class name (as a Java String) and convert to Rust &str
            let class_obj = env
                .get_object_class(&ex)
                .and_then(|cls| {
                    env.call_method(cls, "getName", "()Ljava/lang/String;", &[])
                        .and_then(|v| v.l())
                })
                .unwrap_or_else(|e| {
                    // avoid trying to debug-print JThrowable; print the jni error instead
                    panic!("Failed to get class name for exception: {}", e)
                });

            let class_jstr = env
                .get_string((&class_obj).into())
                .unwrap_or_else(|e| panic!("Failed to get class string from env: {}", e));
            let class = class_jstr.to_str().unwrap_or("<invalid utf8>");

            // Obtain message (as a Java String) and convert to Rust &str
            let message_obj = env
                .call_method(&ex, "getMessage", "()Ljava/lang/String;", &[])
                .and_then(|v| v.l())
                .unwrap_or_else(|e| panic!("Failed to get message for exception: {}", e));

            let message_jstr = env
                .get_string((&message_obj).into())
                .unwrap_or_else(|e| panic!("Failed to get message string from env: {}", e));
            let message = message_jstr.to_str().unwrap_or("<invalid utf8>");

            format!("{}. Java exception occurred: {}: {}", msg, class, message)
        }
        // If exception_occurred() returned an Err, keep original msg
        Err(_) => msg,
    };

    env.throw_new("java/lang/RuntimeException", &msg)
        .unwrap_or_else(|e| panic!("Failed to throw error '{}' as Java exception: {:?}", msg, e));
}
/// The error will be formatted to string as the exception's message.
#[macro_export]
macro_rules! unwrap_or_throw {
    ($env:expr, $res:expr $(, $ret:expr)?) => {
        match $res {
            Ok(x) => x,
            Err(e) => {
                $crate::throw_runtime_exception($env, format!("{e:?}"));
                return $($ret)?;
            }
        }
    };
}
