// Used for testing work in progress features
use crate::{
    TOKIO_RUNTIME, complete_future_exception, complete_future_success_with_fn,
    make_completable_future,
};
use jni::objects::JObject;
use jni::sys::jobject;
use jni::{
    JNIEnv,
    objects::{JClass, JString},
    sys::jstring,
};
use std::time::Duration;
use std::{future::Future, thread};

use librespot_connect::{ConnectConfig, Spirc};
use librespot_core::{Error, Session, SessionConfig, authentication::Credentials};
use librespot_playback::{
    audio_backend,
    config::{AudioFormat, PlayerConfig},
    mixer,
    mixer::{MixerConfig, NoOpVolume},
    player::Player,
};

async fn create_basic_spirc(access_token: String) -> Result<(), Error> {
    let credentials = Credentials::with_access_token(access_token);
    let session = Session::new(SessionConfig::default(), None);

    let backend = audio_backend::find(None).expect("will default to rodio");

    let player = Player::new(
        PlayerConfig::default(),
        session.clone(),
        Box::new(NoOpVolume),
        move || {
            let format = AudioFormat::default();
            let device = None;
            backend(device, format)
        },
    );

    let mixer = mixer::find(None).expect("will default to SoftMixer");

    info!("New spirc!");

    let (spirc, spirc_task): (Spirc, _) = Spirc::new(
        ConnectConfig::default(),
        session,
        credentials,
        player,
        mixer(MixerConfig::default())?,
    )
    .await?;

    Ok(())
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_Debug_debug1(
    mut env: JNIEnv,
    _this: JClass,
    jaccess: JString,
) {
    let rt = match TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            warn!("Failed to initialize session as Tokio Runtime is not initialized!");
            return;
        }
    };

    let token: String = match env.get_string(&jaccess) {
        Ok(s) => s.into(),
        Err(e) => {
            error!("Failed to read access token from JNI: {e}");
            return;
        }
    };

    let token_move = token.clone();

    rt.block_on(async { create_basic_spirc(token_move).await });
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_Debug_debugAsync(
    mut env: JNIEnv,
    _this: JClass,
) -> jobject {
    let (local_cf_obj, global_cf) = make_completable_future!(&mut env);
    let jvm = crate::JVM.get().expect("Failed to get JVM");

    complete_future_success_with_fn!(jvm, global_cf, make_hello_global);

    local_cf_obj.into_raw()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_Debug_debugAsyncErr(
    mut env: JNIEnv,
    _this: JClass,
) -> jobject {
    let (local_cf_obj, global_cf) = make_completable_future!(&mut env);
    let jvm = crate::JVM.get().expect("Failed to get JVM");

    complete_future_exception!(jvm, global_cf, "something went wrong");

    local_cf_obj.into_raw()
}

fn make_hello_global(env: &JNIEnv) -> jni::objects::GlobalRef {
    let local = env
        .new_string("hello from cf rust")
        .expect("failed to create JString");
    env.new_global_ref(local)
        .expect("failed to create GlobalRef")
}
