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

async fn create_spirc() -> Result<(), Error> {
    let player_config = PlayerConfig::default();
    let audio_format = AudioFormat::default();
    let connect_config = ConnectConfig::default();
    let mixer_config = MixerConfig::default();
    let request_options = librespot_connect::LoadRequestOptions::default();

    let sink_builder = audio_backend::find(None).unwrap();
    let mixer_builder = mixer::find(None).unwrap();

    let session = crate::session::SESSION.get().unwrap();
    let credentials = session.cache().unwrap().credentials().unwrap();
    let mixer = mixer_builder(mixer_config)?;

    let player = Player::new(
        player_config,
        session.clone(),
        mixer.get_soft_volume(),
        move || sink_builder(None, audio_format),
    );

    let (spirc, spirc_task) =
        Spirc::new(connect_config, session.clone(), credentials.clone(), player, mixer).await?;

    // these calls can be seen as "queued"
    spirc.activate()?;
    spirc.load(librespot_connect::LoadRequest::from_context_uri(
        "spotify:track:3qhlB30KknSejmIvZZLjOD".to_string(),
        request_options,
    ))?;
    spirc.play()?;

    // starting the connect device and processing the previously "queued" calls
    spirc_task.await;

    Ok(())
}

async fn create_basic_spirc() -> Result<(), Error> {
    let session = crate::session::connect().await.unwrap();
    let backend = audio_backend::find(None).expect("will default to android");

    let player = Player::new(
        PlayerConfig::default(),
        session.clone(), // clone for the player
        Box::new(NoOpVolume),
        move || {
            let format = AudioFormat::default();
            let device = None;
            backend(device, format)
        },
    );

    let mixer = mixer::find(None).expect("will default to SoftMixer");

    info!("New spirc!");

    // IMPORTANT: pull credentials out *before* moving `session` into Spirc::new
    let credentials = session.cache().unwrap().credentials().unwrap();

    // Pass a clone of session into Spirc::new so `session` itself remains available
    let (spirc, spirc_task): (Spirc, _) = Spirc::new(
        ConnectConfig::default(),
        session.clone(), // consume a clone
        credentials,     // already extracted above
        player,
        mixer(MixerConfig::default())?,
    )
    .await?;

    Ok(())
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_Debug_debug1(mut env: JNIEnv, _this: JClass) {
    let rt = match TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => {
            warn!("Failed to initialize session as Tokio Runtime is not initialized!");
            return;
        }
    };

    rt.spawn(async {
        if let Err(e) = create_spirc().await {
            log::error!("create_basic_spirc failed: {:?}", e);
        }
    });
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
