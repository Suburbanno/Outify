package cc.tomko.outify.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.shimmerEffect(): Modifier {
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f)
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 500f, 0f),
        end = Offset(translateAnim, 0f)
    )
    return this.background(brush)
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp),
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier
            .clip(shape)
            .shimmerEffect()
    ) {
        content()
    }
}

@Composable
fun SkeletonTrackRow(
    modifier: Modifier = Modifier,
    showArtwork: Boolean = true,
    showSubtitle: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showArtwork) {
            SkeletonBox(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(6.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp),
                shape = RoundedCornerShape(4.dp)
            )

            if (showSubtitle) {
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(12.dp),
                    shape = RoundedCornerShape(4.dp)
                )
            }
        }
    }
}

@Composable
fun SkeletonAlbumCard(
    modifier: Modifier = Modifier,
    size: Dp = 84.dp
) {
    Column(
        modifier = modifier.width(size)
    ) {
        SkeletonBox(
            modifier = Modifier
                .size(size)
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(14.dp),
            shape = RoundedCornerShape(4.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(12.dp),
            shape = RoundedCornerShape(4.dp)
        )
    }
}

@Composable
fun SkeletonAlbumTracksHeader(
    modifier: Modifier = Modifier,
    imageSize: Dp = 48.dp
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SkeletonBox(
            modifier = Modifier.size(imageSize),
            shape = RoundedCornerShape(8.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(20.dp),
                shape = RoundedCornerShape(4.dp)
            )

            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.25f)
                    .height(14.dp),
                shape = RoundedCornerShape(4.dp)
            )
        }

        SkeletonBox(
            modifier = Modifier.size(24.dp),
            shape = CircleShape
        )
    }
}

@Composable
fun AlbumDetailSkeleton(
    modifier: Modifier = Modifier,
    trackCount: Int = 8
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 200.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, bottom = 8.dp)
                    .fillMaxWidth(0.3f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
        }

        items(trackCount) {
            SkeletonTrackRow()
        }
    }
}

@Composable
fun ArtistDetailSkeleton(
    modifier: Modifier = Modifier,
    trackCount: Int = 5,
    albumCount: Int = 4,
    topPadding: Dp = 200.dp
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = topPadding),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, bottom = 8.dp)
                    .fillMaxWidth(0.4f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
        }

        item {
            SkeletonAlbumTracksHeader()
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, bottom = 8.dp)
                    .fillMaxWidth(0.3f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
        }

        items(trackCount) {
            SkeletonTrackRow()
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, bottom = 16.dp)
                    .fillMaxWidth(0.25f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
        }

        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(albumCount) {
                    SkeletonAlbumCard()
                }
            }
        }
    }
}

@Composable
fun PlaylistDetailSkeleton(
    modifier: Modifier = Modifier,
    trackCount: Int = 8
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 200.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, bottom = 8.dp)
                    .fillMaxWidth(0.3f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
        }

        items(trackCount) {
            SkeletonTrackRow(showSubtitle = false)
        }
    }
}