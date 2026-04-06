package cc.tomko.outify.ui.components.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cc.tomko.outify.ALBUM_COVER_URL
import cc.tomko.outify.core.model.CoverSize
import cc.tomko.outify.core.model.Playlist
import cc.tomko.outify.core.model.Track
import cc.tomko.outify.core.model.getCover
import cc.tomko.outify.data.setting.LocalUiSettings
import cc.tomko.outify.ui.components.SmartImage
import cc.tomko.outify.ui.components.rows.PlaylistRow
import cc.tomko.outify.ui.viewmodel.bottomsheet.AddToPlaylistViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistBottomSheet(
    viewModel: AddToPlaylistViewModel,
    tracks: List<Track>,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
    )

    val coroutineScope = rememberCoroutineScope()

    val playlists by viewModel.ownedPlaylists.collectAsState(initial = emptyList())

    val imageSize = 56.dp
    val previewSize = 40.dp

    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }

    if (selectedPlaylist != null) {
        TrackSelectionBottomSheet(
            viewModel = viewModel,
            tracks = tracks,
            playlist = selectedPlaylist!!,
            onDismiss = { selectedPlaylist = null },
            onConfirm = {
                selectedPlaylist = null
                onDismiss?.invoke()
            }
        )
    } else {
        ModalBottomSheet(
            onDismissRequest = {
                coroutineScope.launch {
                    sheetState.hide()
                    onDismiss?.invoke()
                }
            },
            sheetState = sheetState,
            modifier = modifier,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (tracks.size > 1) "Add ${tracks.size} tracks to playlist" else "Add to playlist",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (tracks.size > 1) {
                    TracksPreviewRowComponent(
                        tracks = tracks,
                        previewSize = previewSize,
                        totalCount = tracks.size
                    )
                } else {
                    val track = tracks.first()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.size(imageSize)
                        ) {
                            val artworkUrl = remember(track) { ALBUM_COVER_URL + (track.album?.getCover(CoverSize.MEDIUM)?.uri ?: "") }
                            SmartImage(
                                url = artworkUrl,
                                contentDescription = "Artwork",
                                modifier = Modifier.fillMaxWidth(),
                                monochrome = LocalUiSettings.current.monochromeTracks
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2
                            )

                            val artists = track.artists

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                artists.forEachIndexed { index, artist ->
                                    Text(
                                        text = artist.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )

                                    if (index < artists.lastIndex) {
                                        Text(
                                            text = ", ",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Available playlists:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    tonalElevation = 16.dp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false),
                    ) {
                        items(
                            items = playlists,
                            key = { it.playlist.id }
                        ) {
                            PlaylistRowWithAdd(
                                tracks = tracks,
                                playlist = it.playlist,
                                artworkUrl = it.artworkUrl,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        selectedPlaylist = it.playlist
                                    }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun PlaylistRowWithAdd(
    tracks: List<Track>,
    playlist: Playlist,
    artworkUrl: String?,
    modifier: Modifier = Modifier,
) {
    val trackIds = tracks.map { it.id }.toSet()

    val existingTracksCount = playlist.contents.count { playlistTrack ->
        playlistTrack.id in trackIds
    }
    val missingTracksCount = tracks.count() - existingTracksCount

    PlaylistRow(
        playlist = playlist,
        artworkUrl = artworkUrl,
        color = Color.Transparent,
        trailingContent = {
            Row {
                if (missingTracksCount > 0) {
                    Text(
                        text = "+${missingTracksCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null
                )
            }
        },
        modifier = modifier
    )
}

@Composable
private fun TracksPreviewRowComponent(
    tracks: List<Track>,
    previewSize: Dp,
    totalCount: Int,
) {
    val previewsToShow = tracks.take(3)
    val extraCount = totalCount - previewsToShow.size

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
            previewsToShow.forEach { track ->
                val artworkUrl = remember(track) { ALBUM_COVER_URL + (track.album?.getCover(CoverSize.SMALL)?.uri ?: "") }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp,
                    modifier = Modifier.size(previewSize)
                ) {
                    if (artworkUrl.isNotBlank()) {
                        SmartImage(
                            url = artworkUrl,
                            contentDescription = "Preview artwork",
                            modifier = Modifier.fillMaxSize(),
                            monochrome = LocalUiSettings.current.monochromeTracks
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$totalCount tracks selected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (extraCount > 0) {
                Text(
                    text = "+ $extraCount more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}