package cc.tomko.outify.ui.components.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.tomko.outify.ALBUM_COVER_URL
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.data.CoverSize
import cc.tomko.outify.data.getCover
import cc.tomko.outify.ui.components.TrackRow
import cc.tomko.outify.ui.viewmodel.player.QueueViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SharedTransitionScope.QueueBottomSheet(
    sheetState: SheetState,
    viewModel: QueueViewModel,
    onDismissRequest: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val queueState by viewModel.queueState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val currentTrack = OutifyApplication.playbackStateHolder.state.collectAsState().value.currentTrack

    // Load queue when sheet is opened
    LaunchedEffect(viewModel) {
        viewModel.loadQueue(currentTrack)
    }

    // Local state for drag-and-drop operations
    var localTracks by remember {
        mutableStateOf(queueState.tracks)
    }

    var isDragging by remember { mutableStateOf(false) }

    // Sync local tracks with viewModel state only when not dragging
    LaunchedEffect(queueState.tracks) {
        if (!isDragging) {
            localTracks = queueState.tracks
        }
    }

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to ->
            val fromIndex = from.index - 1 // Account for header
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

    // Monitor scroll position for lazy loading (with debouncing to reduce lag)
    LaunchedEffect(listState, isDragging) {
        snapshotFlow {
            if (isDragging) {
                // Don't trigger loading while dragging
                null
            } else {
                val layoutInfo = listState.layoutInfo
                val firstVisibleIndex = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                firstVisibleIndex to lastVisibleIndex
            }
        }
            .distinctUntilChanged()
            .collect { indices ->
                indices?.let { (first, last) ->
                    // Account for header item (index 0)
                    val adjustedFirst = maxOf(0, first - 1)
                    val adjustedLast = maxOf(0, last - 1)
                    viewModel.onScrollPositionChanged(adjustedFirst, adjustedLast, currentTrack)
                }
            }
    }

    // Auto-scroll to current track on initial load
    LaunchedEffect(queueState.tracks, queueState.currentIndex) {
        if (queueState.tracks.isNotEmpty() && !queueState.isLoading) {
            val index = queueState.currentIndex.coerceIn(queueState.tracks.indices)
            // Add 1 to account for header item
            listState.scrollToItem(index + 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = if (queueState.tracks.isEmpty() && !queueState.isLoading) {
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        } else {
            Modifier
                .fillMaxWidth()
                .heightIn(min = 300.dp)
                .wrapContentHeight()
        },
        tonalElevation = 3.dp,
    ) {
        Column {
            // Loading indicator at the top
            if (queueState.isLoadingPrevious) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            when {
                queueState.isLoading -> {
                    // Initial loading state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Loading queue...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                queueState.tracks.isEmpty() -> {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = "Queue empty",
                                modifier = Modifier.size(70.dp)
                            )
                            Text(
                                text = "The queue is empty",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }

                else -> {
                    // Queue content
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        // Header
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = "Queue icon",
                                    modifier = Modifier
                                        .clip(MaterialShapes.Cookie9Sided.toShape())
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(16.dp)
                                        .size(20.dp)
                                )
                                Text(
                                    text = "Queue",
                                    style = MaterialTheme.typography.headlineMediumEmphasized,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(start = 16.dp)
                                )

                                Text(
                                    text = "${queueState.totalSize} songs",
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }

                        // Track items
                        items(
                            items = localTracks,
                            key = { it.id },
                            contentType = { "track" },
                        ) { item ->
                            val trackIndex = remember(localTracks) {
                                localTracks.indexOf(item)
                            }
                            val artworkUrl = remember(item.track.album?.getCover(CoverSize.MEDIUM)) {
                                ALBUM_COVER_URL +
                                        item.track.album?.getCover(CoverSize.MEDIUM)
                            }

                            ReorderableItem(
                                reorderState,
                                key = item.id
                            ) { isDraggingItem ->
                                val elevation by animateDpAsState(
                                    if (isDraggingItem) 4.dp else 0.dp,
                                    label = "elevation"
                                )

                                Surface(
                                    shadowElevation = elevation
                                ) {
                                    Row(
                                        modifier = Modifier.padding(start = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            modifier = Modifier.draggableHandle(
                                                onDragStarted = {
                                                    hapticFeedback.performHapticFeedback(
                                                        HapticFeedbackType.LongPress
                                                    )
                                                },
                                                onDragStopped = {
                                                    hapticFeedback.performHapticFeedback(
                                                        HapticFeedbackType.LongPress
                                                    )
                                                }
                                            ),
                                            onClick = {}
                                        ) {
                                            Icon(
                                                Icons.Default.DragIndicator,
                                                contentDescription = "Reorder"
                                            )
                                        }
                                        TrackRow(
                                            title = item.track.name,
                                            artist = item.track.artists.joinToString { it.name },
                                            artworkUrl = artworkUrl,
                                            onRowClick = remember(item.track.uri) {
                                                {
                                                    coroutineScope.launch {
                                                        OutifyApplication.spirc.load(item.track.uri)
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

            // Loading indicator at the bottom
            if (queueState.isLoadingNext) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Error message
            queueState.error?.let { error ->
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberQueueBottomSheetState(): QueueBottomSheetController {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    val visible = remember { mutableStateOf(false) }

    return remember {
        QueueBottomSheetController(
            sheetState = sheetState,
            visible = visible
        )
    }
}

class QueueBottomSheetController @OptIn(ExperimentalMaterial3Api::class) constructor(
    val sheetState: SheetState,
    val visible: MutableState<Boolean>
) {
    fun show() {
        visible.value = true
    }

    fun hide() {
        visible.value = false
    }
}