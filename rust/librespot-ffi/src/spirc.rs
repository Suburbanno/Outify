use librespot_connect::{ConnectConfig, LoadRequest, Spirc};
use librespot_core::{Session, authentication::Credentials};
use librespot_playback::{
    config::{AudioFormat, PlayerConfig},
    mixer::{self, Mixer, MixerConfig},
    player::Player,
};
use tokio::task::JoinHandle;

pub struct SpircRuntime {
    spirc: Spirc,
    task: JoinHandle<()>,
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

        Ok(Self { spirc, task })
    }

    pub async fn play(self) -> Result<(), librespot_core::Error>{
        self.spirc.play()
    }

    pub async fn play_pause(self) -> Result<(), librespot_core::Error>{
        self.spirc.play_pause()
    }

    pub async fn pause(self) -> Result<(), librespot_core::Error>{
        self.spirc.pause()
    }

    pub async fn load(self, req: LoadRequest) -> Result<(), librespot_core::Error>{
        self.spirc.load(req)
    }

    // This function keeps the SpircTask running.
    // Returns only when SpircTask exits
    pub async fn run_indefinitely(self) -> Result<(), Box<dyn std::error::Error>> {
        self.task.await?;
        Err("Spirc task exited unexpectedly".into())
    }
}
