use std::sync::Arc;

use librespot_connect::{ConnectConfig, LoadRequest, Spirc};
use librespot_core::{Session, SpotifyUri, authentication::Credentials, spclient::TransferRequest};
use librespot_playback::{
    config::{AudioFormat, PlayerConfig},
    mixer::{self, MixerConfig},
    player::Player,
};
use tokio::{sync::Mutex, task::JoinHandle};

pub struct SpircRuntime {
    spirc: Arc<Spirc>,
    task: Mutex<Option<JoinHandle<()>>>,
}

impl SpircRuntime {
    pub async fn new(
        session: &Session,
        credentials: Credentials,
    ) -> Result<Self, Box<dyn std::error::Error>> {
        let player_config = PlayerConfig::default();
        let audio_format = AudioFormat::S16;
        let mixer_config = MixerConfig::default();

        let sink_builder =
            librespot_playback::audio_backend::find(None).ok_or("no audio backend available")?;
        let mixer_builder = mixer::find(None).ok_or("no mixer builder available")?;

        let mixer_impl = mixer_builder(mixer_config)?;

        let player = Player::new(
            player_config,
            session.clone(),
            mixer_impl.get_soft_volume(),
            move || sink_builder(None, audio_format),
        );

        let connect_config = ConnectConfig {
            name: "Outify".to_string(), // TODO: Make configurable?
            ..Default::default()
        };

        let (spirc, spirc_future) = Spirc::new(
            connect_config,
            session.clone(),
            credentials,
            player,
            mixer_impl,
        )
        .await?;

        let task = tokio::spawn(spirc_future);

        Ok(Self {
            spirc: Arc::new(spirc),
            task: Mutex::new(Some(task)),
        })
    }

    pub fn play(&self) -> Result<(), librespot_core::Error> {
        self.spirc.play()
    }

    pub fn play_pause(&self) -> Result<(), librespot_core::Error> {
        self.spirc.play_pause()
    }

    pub fn pause(&self) -> Result<(), librespot_core::Error> {
        self.spirc.pause()
    }

    pub fn load(&self, req: LoadRequest) -> Result<(), librespot_core::Error> {
        self.spirc.load(req)
    }

    pub fn add_to_queue(&self, uri: SpotifyUri) -> Result<(), librespot_core::error::Error> {
        self.spirc.add_to_queue(uri)
    }

    pub fn set_volume(&self, volume: u16) -> Result<(), librespot_core::error::Error> {
        self.spirc.set_volume(volume)
    }

    pub fn activate(&self) -> Result<(), librespot_core::Error> {
        self.spirc.activate()
    }

    pub fn transfer(&self) -> Result<(), librespot_core::Error> {
        // TODO: Make configurable from Java?
        let options = librespot_core::dealer::protocol::TransferOptions {
            ..Default::default()
        };
        let request = TransferRequest {
            transfer_options: options,
        };
        self.spirc.transfer(Some(request))
    }
}
