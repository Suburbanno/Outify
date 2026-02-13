package cc.tomko.outify.playback

import android.app.Application
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST
import androidx.media3.common.Player.STATE_BUFFERING
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Player.STATE_READY
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.SimpleBasePlayer.MediaItemData
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import cc.tomko.outify.ALBUM_COVER_URL
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.core.spirc.Spirc
import cc.tomko.outify.data.CoverSize
import cc.tomko.outify.data.Track
import cc.tomko.outify.data.getCover
import cc.tomko.outify.playback.callbacks.PlayerEventCallback
import cc.tomko.outify.playback.model.PlayState
import cc.tomko.outify.playback.model.RepeatMode
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@UnstableApi
class Player(
    application: Application,
    val stateHolder: PlaybackStateHolder = PlaybackStateHolder(),
): SimpleBasePlayer(application.mainLooper) {
    private val json = Json { ignoreUnknownKeys = true }
    private val playerJob = SupervisorJob()

    private val scope = CoroutineScope(
        Dispatchers.Main.immediate + playerJob
    )

    var engine: AudioEngine = AudioEngine(application.applicationContext, object: PlayerEventCallback {
        override fun onTrackChange(spotify_uri: String, json_raw: String) {
            scope.launch {
                val track: Track = json.decodeFromString(json_raw)
                stateHolder.setTrack(track)
                invalidateState()
            }
        }

        override fun onPositionUpdate(
            spotify_uri: String,
            position_ms: Long,
            json_raw: String
        ) {
            if(stateHolder.state.value.currentTrack?.id != spotify_uri) {
                onTrackChange(spotify_uri, json_raw)
            }

            scope.launch {
                stateHolder.seekTo(position_ms.toDuration(DurationUnit.MILLISECONDS))
                invalidateState()
            }
        }

        override fun onPlayingStatus(playing: Boolean) {
            scope.launch {
                stateHolder.setPlaying(playing)
                invalidateState()
            }
        }
    })

    init {
        scope.launch {
            stateHolder.state.collect {
                invalidateState()
            }
        }
    }

    override fun getState(): State {
        val ps = stateHolder.state.value

        val playlist: List<MediaItemData> = ps.currentTrack?.let { track ->
            listOf(
                MediaItemData.Builder(track.id)
                    .setMediaItem(track.toMediaItem())
                    .setDurationUs(track.duration * 1000)
                    .setDefaultPositionUs(0)
                    .setIsSeekable(true)
                    .build()
            )
        } ?: emptyList()

        val playbackState = when {
            ps.state == PlayState.BUFFERING -> STATE_BUFFERING
            playlist.isEmpty() -> STATE_IDLE
            ps.isPlaying -> STATE_READY
            else -> STATE_READY  // Still ready even when paused
        }

        return State.Builder()
            .setPlaybackState(playbackState)
            .setAvailableCommands(determineCommands(playlist.isNotEmpty()))
            .setPlayWhenReady(ps.isPlaying, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackParameters(PlaybackParameters(ps.playbackSpeed))
            .setCurrentMediaItemIndex(0)
            .setContentPositionMs(ps.position.active.inWholeMilliseconds)
            .setIsLoading(ps.state == PlayState.BUFFERING)
            .setPlaylist(playlist)
            .build()
    }

    // Instead of overriding play() you implement the handler:
    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        // TODO: More robust handling?
        if (playWhenReady) {
            Spirc.playerPlay()
        } else {
            Spirc.playerPause()
        }
        // return a completed future - if your controller needs async work, return a future that completes later
        return com.google.common.util.concurrent.Futures.immediateVoidFuture()
    }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
//        spirc.seekTo(mediaItemIndex, positionMs)
        return com.google.common.util.concurrent.Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        Spirc.playerPause() //TODO: Implement playerStop
        return com.google.common.util.concurrent.Futures.immediateVoidFuture()
    }

    public override fun handleRelease(): ListenableFuture<*> {
        playerJob.cancel()
        engine.releaseAudioTrack()
        // TODO: tell spirc to cleanup
        return com.google.common.util.concurrent.Futures.immediateVoidFuture()
    }

    override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> {
//        spirc.setRepeatMode(when (repeatMode) {
//            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
//            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
//            else -> RepeatMode.NONE
//        })
        return com.google.common.util.concurrent.Futures.immediateVoidFuture()
    }

    override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean): ListenableFuture<*> {
//        spirc.setShuffle(shuffleModeEnabled)
        return com.google.common.util.concurrent.Futures.immediateVoidFuture()
    }

    override fun handleSetPlaybackParameters(playbackParameters: PlaybackParameters): ListenableFuture<*> {
//        spirc.setPlaybackSpeed(playbackParameters.speed)
        return com.google.common.util.concurrent.Futures.immediateVoidFuture()
    }

    private fun determineCommands(hasMedia: Boolean): Player.Commands {
        val builder = Player.Commands.Builder()
            .add(COMMAND_PLAY_PAUSE)

        if (hasMedia) {
            builder.addAll(
                COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                COMMAND_SET_REPEAT_MODE,
                COMMAND_SET_SHUFFLE_MODE,
                COMMAND_STOP
            )
        }

        return builder.build()
    }
}

fun Track.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(name)
        .setArtist(artists.joinToString { it.name })
        .setAlbumTitle(album?.name)
        .setTotalTrackCount(album?.tracks?.size ?: 0)
        .setArtworkUri((ALBUM_COVER_URL + album?.getCover(CoverSize.MEDIUM)).toUri())
        .setDurationMs(duration.takeIf { it > 0 })
        .build()

    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)
        .setMediaMetadata(metadata)
        .build()
}

