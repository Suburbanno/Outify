package cc.tomko.outify.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LocalContentColor

@Composable
fun AudioBarsIndicator(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 4,
    barWidth: Dp = 4.dp,
    barHeight: Dp = 20.dp,
    spacing: Dp = 4.dp,
    color: Color = LocalContentColor.current,
) {
    val minFraction = 0.18f
    val maxFraction = 1.0f

    val transition = rememberInfiniteTransition()

    val animStates: List<State<Float>> = List(barCount) { index ->
        transition.animateFloat(
            initialValue = minFraction,
            targetValue = maxFraction,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 600 + index * 120,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(index * 80)
            )
        )
    }

    val idleState by animateFloatAsState(targetValue = minFraction, animationSpec = tween(300))

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        for (i in 0 until barCount) {
            val fraction = if (isPlaying) animStates[i].value else idleState
            Column(
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .width(barWidth)
                    .height(barHeight)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight * fraction)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(color)
                )
            }
        }
    }
}
