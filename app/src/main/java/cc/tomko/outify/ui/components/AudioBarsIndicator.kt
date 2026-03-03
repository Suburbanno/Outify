package cc.tomko.outify.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LocalContentColor
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

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
    val idleFraction = 0.18f
    val minFraction = 0.15f
    val maxFraction = 1.0f

    val animatables = remember {
        List(barCount) { Animatable(idleFraction) }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            animatables.forEachIndexed { index, anim ->
                launch {
                    val initialDelay = index * 60L
                    kotlinx.coroutines.delay(initialDelay)
                    while (isActive) {
                        val target = Random.nextFloat() * (maxFraction - minFraction) + minFraction

                        val duration = Random.nextInt(140, 520) // ms
                        anim.animateTo(
                            targetValue = target,
                            animationSpec = tween(
                                durationMillis = duration,
                                easing = LinearEasing
                            )
                        )

                        val pause = Random.nextLong(30L, 150L)
                        kotlinx.coroutines.delay(pause)
                    }
                }
            }
        } else {
            animatables.forEach { anim ->
                launch {
                    anim.animateTo(targetValue = idleFraction, animationSpec = tween(240))
                }
            }
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        animatables.forEach { anim ->
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(barHeight),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight * anim.value)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(color)
                )
            }
        }
    }
}
