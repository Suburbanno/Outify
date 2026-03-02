package cc.tomko.outify.playback

import android.app.Application
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import cc.tomko.outify.ALBUM_COVER_URL
import cc.tomko.outify.core.Spirc.SpircWrapper
import cc.tomko.outify.data.CoverSize
import cc.tomko.outify.data.Track
import cc.tomko.outify.data.getCover
import cc.tomko.outify.playback.callbacks.PlayerEventCallback
import cc.tomko.outify.playback.model.PlayState
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Singleton
@UnstableApi
class Player @Inject constructor(
    application: Application,
    val stateHolder: PlaybackStateHolder,
    val spirc: SpircWrapper,
): SimpleBasePlayer(application.mainLooper) {
    private val json = Json { ignoreUnknownKeys = true }
    private val playerJob = SupervisorJob()

    private val scope = CoroutineScope(
        Dispatchers.Main.immediate + playerJob
    )

    var engine: AudioEngine = AudioEngine(application.applicationContext, object: PlayerEventCallback {
        override fun onTrackChange(spotify_uri: String, json: String) {
            scope.launch {
                val track: Track = this@Player.json.decodeFromString(json)
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
                    .setDurationUs(
                        if (track.duration > 0) track.duration * 1000L else C.TIME_UNSET
                    )
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
            .setCurrentMediaItemIndex(if (playlist.isNotEmpty()) 0 else C.INDEX_UNSET)
            .setContentPositionMs(ps.position.active.inWholeMilliseconds)
            .setIsLoading(ps.state == PlayState.BUFFERING)
            .setPlaylist(playlist)
            .build()
    }

    // Instead of overriding play() you implement the handler:
    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        // TODO: More robust handling?
        if (playWhenReady) {
            spirc.playerPlay()
        } else {
            spirc.playerPause()
        }
        // return a completed future - if your controller needs async work, return a future that completes later
        return com.google.common.util.concurrent.Futures.immediateVoidFuture()
    }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
//        spirc.seekTo(mediaItemIndex, positionMs)
        when (seekCommand) {
            COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> spirc.playerPrevious()
            COMMAND_SEEK_TO_PREVIOUS -> spirc.playerPrevious()

            COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> spirc.playerNext()
            COMMAND_SEEK_TO_NEXT -> spirc.playerNext()

            COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM -> {
                scope.launch {
                    spirc.seekTo(positionMs)
                }
            }
        }
        return com.google.common.util.concurrent.Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        spirc.playerPause() //TODO: Implement playerStop
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
            .add(COMMAND_GET_CURRENT_MEDIA_ITEM)

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

