package cc.tomko.outify.ui.components.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.ui.components.TrackRow
import cc.tomko.outify.ui.viewmodel.player.QueueViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SharedTransitionScope.QueueBottomSheet(
    sheetState: SheetState,
    viewModel: QueueViewModel,
    onDismissRequest: () -> Unit
) {
    val queueState by viewModel.queueState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.loadQueue()
    }

    val listState = rememberLazyListState()
    LaunchedEffect(queueState.tracks, queueState.currentIndex) {
        if (queueState.tracks.isNotEmpty()) {
            val index = queueState.currentIndex.coerceIn(queueState.tracks.indices)
            val viewportItems = queueState.tracks.size
            val scrollIndex = (index - viewportItems / 2).coerceAtLeast(0)
            listState.scrollToItem(scrollIndex)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = if (queueState.tracks.isEmpty()) {
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
        if (queueState.tracks.isEmpty()) {
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
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
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
                            text = "${queueState.tracks.size} songs",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                items(
                    items = queueState.tracks,
                    key = { it.uri },
                    contentType = { "track" },
                ) { track ->
                    val artworkUrl =
                        (OutifyApplication.ALBUM_COVER_URL + track.album?.covers?.firstOrNull()?.uri.orEmpty())

                    TrackRow(
                        title = track.name,
                        artist = track.artists.joinToString { it.name },
                        artworkUrl = artworkUrl,
                        isSelected = false,
                        onRowClick = remember(track.uri) { { OutifyApplication.spirc.load(track.uri) } },
                        onArtistClick = {},
                        sharedTransitionKey = null
                    )
                }
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

