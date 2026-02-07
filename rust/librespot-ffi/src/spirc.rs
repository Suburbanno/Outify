use std::sync::Arc;

use librespot_connect::{ConnectConfig, LoadRequest, Spirc};
use librespot_core::{Session, SpotifyUri, authentication::Credentials, spclient::TransferRequest};
use librespot_playback::{
    config::{AudioFormat, PlayerConfig},
    mixer::{self, MixerConfig},
    player::{Player, PlayerEvent},
};
use tokio::{
    sync::{Mutex, broadcast, mpsc, watch},
    task::JoinHandle,
};

pub struct SpircRuntime {
    spirc: Arc<Spirc>,
    task: Mutex<Option<JoinHandle<()>>>,
}

impl SpircRuntime {
    pub async fn new(
        session: &Session,
        credentials: Credentials,
    ) -> Result<Self, Box<dyn std::error::Error>> {
        let player_config = PlayerConfig {
            // TODO: Make configurable from app
            position_update_interval: Some(std::time::Duration::from_millis(5_000)),
            bitrate: librespot_playback::config::Bitrate::Bitrate320,
            gapless: true,
            normalisation: false,
            ..Default::default()
        };
        let audio_format = AudioFormat::S16;
        let mixer_config = MixerConfig {
            volume_ctrl: librespot_playback::config::VolumeCtrl::Linear,
            ..Default::default()
        };

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

        let (event_tx, event_rx) = mpsc::channel::<PlayerEvent>(64);

        let (spirc, spirc_future) = Spirc::new(
            connect_config,
            session.clone(),
            credentials,
            player,
            mixer_impl,
            Some(event_tx.clone()),
        )
        .await?;

        let task = tokio::spawn(spirc_future);

        // Handling received Player Events
        tokio::spawn(async move {
            let mut rx = event_rx;
            while let Some(ev) = rx.recv().await {
                handle_event(ev);
            }
            info!("SpircRuntime event receiver closed");
        });

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

    pub fn next(&self) -> Result<(), librespot_core::Error> {
        self.spirc.next()
    }

    pub fn prev(&self) -> Result<(), librespot_core::Error> {
        self.spirc.prev()
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

    pub async fn shutdown(&self) {}

    pub async fn prev_tracks(&self) -> Result<Vec<librespot_protocol::player::ProvidedTrack>, librespot_core::Error> {
        self.spirc.prev_tracks().await
            .ok_or_else(|| librespot_core::Error::internal("Spirc task not available"))
    }

    pub async fn next_tracks(&self) -> Result<Vec<librespot_protocol::player::ProvidedTrack>, librespot_core::Error> {
        self.spirc.next_tracks().await
            .ok_or_else(|| librespot_core::Error::internal("Spirc task not available"))
    }
}

// Handles each player event accordingly
fn handle_event(event: PlayerEvent) {
    match event {
        PlayerEvent::Playing {
            play_request_id,
            ref track_id,
            position_ms,
        } => {
            crate::jni_utils::playback::on_player_position_update(position_ms, track_id.clone());
            crate::jni_utils::playback::on_player_status(true);
        }

        PlayerEvent::TrackChanged { audio_item } => {
            crate::jni_utils::playback::on_player_track_update(audio_item.track_id.clone());
        }

        PlayerEvent::Paused { play_request_id, ref track_id, position_ms } => {
            crate::jni_utils::playback::on_player_position_update(position_ms, track_id.clone());
            crate::jni_utils::playback::on_player_status(false);
        }

        PlayerEvent::Seeked { play_request_id, track_id, position_ms } => {
            crate::jni_utils::playback::on_player_position_update(position_ms, track_id.clone());
        }
        
        PlayerEvent::PositionChanged { play_request_id, track_id, position_ms } => {
            crate::jni_utils::playback::on_player_position_update(position_ms, track_id.clone());
        }

        PlayerEvent::SessionConnected { connection_id, user_name } => {
            info!("User {} connected session {}",user_name, connection_id);
        }
        PlayerEvent::SessionDisconnected { connection_id, user_name } => {
            info!("User {} disconnected session {}",user_name, connection_id);
        }
        PlayerEvent::TimeToPreloadNextTrack { play_request_id, track_id } => {
            info!("Its time to preload {}", track_id);
        }
        PlayerEvent::AddedToQueue { track_id } => {
            info!("Track added to queue:  {}", track_id);
        }
        _ => {
            // Not yet implemented
        }
    }
}
