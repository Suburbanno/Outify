package cc.tomko.outify.ui.components.player

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.tomko.outify.ALBUM_COVER_URL
import cc.tomko.outify.data.Artist
import cc.tomko.outify.data.CoverSize
import cc.tomko.outify.data.Track
import cc.tomko.outify.data.getCover
import cc.tomko.outify.ui.components.rows.SwipeableTrackRowConfigured
import cc.tomko.outify.ui.components.rows.TrackRow
import cc.tomko.outify.ui.viewmodel.player.QueueViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SharedTransitionScope.QueueBottomSheet(
    sheetState: SheetState,
    viewModel: QueueViewModel,
    onDismissRequest: () -> Unit,
    onArtistClick: (Artist) -> Unit,
    onArtworkClick: (Track) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val queueState by viewModel.queueState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val spirc = viewModel.spirc

    val isPlaybackPlaying by viewModel.isPlaying().collectAsState(initial = false)
    val currentTrack by viewModel.currentTrack().collectAsState(initial = null)

    val likedTracksId by viewModel.likedTrackIds.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.loadQueue(currentTrack)
    }

    var localTracks by remember { mutableStateOf(queueState.tracks) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(queueState.tracks) {
        if (!isDragging) {
            localTracks = queueState.tracks
        }
    }

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to ->
            val fromIndex = from.index - 1
            val toIndex = to.index - 1
            if (fromIndex >= 0 && toIndex >= 0 &&
                fromIndex in localTracks.indices &&
                toIndex in localTracks.indices
            ) {
                localTracks = localTracks.toMutableList().apply {
                    add(toIndex, removeAt(fromIndex))
                }
                isDragging = true
            }
        },
        lazyListState = listState
    )

    LaunchedEffect(listState, isDragging) {
        snapshotFlow {
            if (isDragging) null
            else {
                val info = listState.layoutInfo
                val first = info.visibleItemsInfo.firstOrNull()?.index ?: 0
                val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                first to last
            }
        }
            .distinctUntilChanged()
            .collect { indices ->
                indices?.let { (first, last) ->
                    viewModel.onScrollPositionChanged(
                        maxOf(0, first - 1),
                        maxOf(0, last - 1),
                        currentTrack
                    )
                }
            }
    }

    LaunchedEffect(queueState.tracks, queueState.currentIndex) {
        if (queueState.tracks.isNotEmpty() && !queueState.isLoading) {
            listState.scrollToItem(queueState.currentIndex.coerceIn(queueState.tracks.indices) + 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (queueState.tracks.isEmpty() && !queueState.isLoading)
                    Modifier.wrapContentHeight()
                else
                    Modifier.heightIn(min = 300.dp).wrapContentHeight()
            ),
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = "Queue",
                    modifier = Modifier
                        .clip(MaterialShapes.Cookie9Sided.toShape())
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(12.dp)
                        .size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Queue",
                        style = MaterialTheme.typography.headlineMediumEmphasized,
                        fontWeight = FontWeight.Black,
                    )
                    if (!queueState.isLoading && queueState.totalSize > 0) {
                        Text(
                            text = "${queueState.totalSize} songs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (queueState.isLoadingPrevious) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when {
                queueState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Loading queue…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                queueState.tracks.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "The queue is empty",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Spacer item to preserve index offset for reorder
                        item { Spacer(modifier = Modifier.height(0.dp)) }

                        items(
                            items = localTracks,
                            key = { it.id },
                            contentType = { "track" },
                        ) { item ->
                            val artworkUrl = remember(item.track.album?.getCover(CoverSize.MEDIUM)) {
                                ALBUM_COVER_URL + item.track.album?.getCover(CoverSize.MEDIUM)?.uri
                            }
                            val isCurrentTrack = item.track.id == currentTrack?.id

                            ReorderableItem(reorderState, key = item.id) { isDraggingItem ->
                                val elevation by animateDpAsState(
                                    targetValue = if (isDraggingItem) 4.dp else 0.dp,
                                    label = "elevation"
                                )

                                Surface(
                                    shadowElevation = elevation,
                                    shape = RoundedCornerShape(8.dp),
                                    color = when {
                                        isDraggingItem  -> MaterialTheme.colorScheme.surfaceVariant
                                        isCurrentTrack  -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        else            -> Color.Transparent
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        IconButton(
                                            modifier = Modifier.draggableHandle(
                                                onDragStarted = {
                                                    hapticFeedback.performHapticFeedback(
                                                        HapticFeedbackType.LongPress
                                                    )
                                                    isDragging = true
                                                },
                                                onDragStopped = {
                                                    hapticFeedback.performHapticFeedback(
                                                        HapticFeedbackType.LongPress
                                                    )
                                                    isDragging = false
                                                }
                                            ),
                                            onClick = {}
                                        ) {
                                            Icon(
                                                Icons.Default.DragIndicator,
                                                contentDescription = "Reorder",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        SwipeableTrackRowConfigured(
                                            track = item.track,
                                            currentTrack = currentTrack,
                                            isPlaybackPlaying = isPlaybackPlaying,
                                            onRowClick = remember(item.track.uri) {
                                                {
//                                                    spirc.load(album.uri, item.track.uri)
                                                }
                                            },
                                            isLiked = item.track.id in likedTracksId,
                                            onArtistClick = { onArtistClick(it) },
                                            onArtworkClick = { onArtworkClick(item.track) }
                                        )
                                        TrackRow(
                                            title = item.track.name,
                                            artists = item.track.artists,
                                            artworkUrl = artworkUrl,
                                            onRowClick = remember(item.track.uri) {
                                                {
                                                    coroutineScope.launch {
                                                        spirc.load(item.track.uri)
                                                    }
                                                }
                                            },
                                            onArtistClick = {},
                                            sharedTransitionKey = null,
                                            color = Color.Transparent
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (queueState.isLoadingNext) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            queueState.error?.let { error ->
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberQueueBottomSheetState(): QueueBottomSheetController {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val visible = remember { mutableStateOf(false) }
    return remember { QueueBottomSheetController(sheetState, visible) }
}

class QueueBottomSheetController @OptIn(ExperimentalMaterial3Api::class) constructor(
    val sheetState: SheetState,
    val visible: MutableState<Boolean>
) {
    fun show() { visible.value = true }
    fun hide() { visible.value = false }
}