package cc.tomko.outify.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.tomko.outify.OutifyApplication
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware

enum class TrackRowDensity { Compact, Default, Spacious }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
    title: String,
    artist: String,
    artworkUrl: String?,
    isPlaying: Boolean = false,
    isSelected: Boolean = false,
    density: TrackRowDensity = TrackRowDensity.Default,
    trailingContent: @Composable (() -> Unit)? = null,

    // Interaction handlers
    onRowClick: (() -> Unit)? = null,
    onRowLongClick: (() -> Unit)? = null,
    onArtworkClick: (() -> Unit)? = null,
    onTitleClick: (() -> Unit)? = null,
    onArtistClick: (() -> Unit)? = null,

    contentDescription: String? = null,

    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageDp: Dp = when (density) {
        TrackRowDensity.Compact -> 40.dp
        TrackRowDensity.Default -> 56.dp
        TrackRowDensity.Spacious -> 72.dp
    }

    val imageSizePx = with(LocalDensity.current) { imageDp.roundToPx() }

    val imageRequest = remember(artworkUrl, imageSizePx) {
        ImageRequest.Builder(context)
            .data(artworkUrl)
            .size(imageSizePx)
            .allowHardware(true)
            .build()
    }

    val combinedModifier = if (onRowClick != null || onRowLongClick != null) {
        modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onRowClick?.invoke() },
                onLongClick = { onRowLongClick?.invoke() }
            )
    } else {
        modifier.fillMaxWidth()
    }

    val imageLoader = (LocalContext.current.applicationContext as OutifyApplication).imageLoader

    Surface(
        modifier = combinedModifier.semantics {
            contentDescription?.let { this.contentDescription = it }
        },
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = when (density) {
                    TrackRowDensity.Compact -> 6.dp
                    TrackRowDensity.Default -> 8.dp
                    TrackRowDensity.Spacious -> 12.dp
                })
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier
                .size(imageDp)
                .clip(RoundedCornerShape(6.dp))
            ) {
                AsyncImage(
                    model = imageRequest,
                    imageLoader = imageLoader,
                    contentDescription = "Artwork for $title",
                    modifier = Modifier
                        .matchParentSize()
                        .then(
                            if (onArtworkClick != null) {
                                Modifier.combinedClickable(
                                    onClick = { onArtworkClick() },
                                    onLongClick = {}
                                )
                            } else Modifier
                        )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = when(density){
                        TrackRowDensity.Compact -> 14.sp
                        TrackRowDensity.Default -> 16.sp
                        TrackRowDensity.Spacious -> 18.sp
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (onTitleClick != null) {
                                Modifier.combinedClickable(
                                    onClick = { onTitleClick() },
                                    onLongClick = {}
                                )
                            } else Modifier
                        )
                        .testTag("trackrow.title")
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .then(
                            if (onArtistClick != null) {
                                Modifier.combinedClickable(
                                    onClick = { onArtistClick() },
                                    onLongClick = {}
                                )
                            } else Modifier
                        )
                        .testTag("trackrow.artist")
                )
            }

            if (trailingContent != null) {
                Box(
                    modifier = Modifier.wrapContentWidth(Alignment.End),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    trailingContent()
                }
            } else {
                if (isPlaying) {
                    // Playing indicator
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.PlayArrow,
                        contentDescription = "Playing",
                        modifier = Modifier.size(20.dp)
                    )
                } else if (isSelected) {
                    Checkbox(checked = true, onCheckedChange = null)
                }
            }
        }
    }
}