package cc.tomko.outify.ui.notifications

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun InAppNotificationHost(
    modifier: Modifier = Modifier,
    hostPaddingBottom: Int = 0,
    maxWidthFraction: Float = 0.92f,
    contentAlignmentBottom: Alignment = Alignment.BottomCenter,
    contentAlignmentTop: Alignment = Alignment.TopCenter,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val events = InAppNotificationController.events

    var current by remember { mutableStateOf<NotificationSpec?>(null) }
    var dismissJob by remember { mutableStateOf<Job?>(null) }

    val offsetY = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    var bannerHeightPx by remember { mutableIntStateOf(0) }

    fun startOffsetForPlacement(placement: NotificationPlacement, height: Float): Float =
        if (placement == NotificationPlacement.Bottom) height else -height

    LaunchedEffect(events) {
        events.collectLatest { spec ->
            if (current != null) {
                dismissJob?.cancel()
                val exit = startOffsetForPlacement(current!!.placement, bannerHeightPx + 40f)
                offsetY.animateTo(
                    targetValue = exit,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                current = null
                delay(80)
            }

            current = spec

            val start = startOffsetForPlacement(spec.placement, (bannerHeightPx + 60f).coerceAtLeast(120f))
            offsetY.snapTo(start)
            alpha.snapTo(0f)

            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )

            alpha.animateTo(1f)

            // schedule auto-dismiss
            dismissJob = scope.launch {
                delay(spec.durationMillis)
                val exit = startOffsetForPlacement(spec.placement, bannerHeightPx + 60f)
                offsetY.animateTo(
                    targetValue = exit,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                current = null
            }
        }
    }

    Box(modifier = modifier) {
        val placementAlignment =
            if (current?.placement == NotificationPlacement.Top) contentAlignmentTop else contentAlignmentBottom

        current?.let { spec ->
            Box(
                modifier = Modifier.align(placementAlignment)
            ) {
                BannerLayout(
                    spec = spec,
                    offsetY = offsetY,
                    alpha = alpha,
                    maxWidthFraction = maxWidthFraction,
                    onMeasured = { bannerHeightPx = it },
                    onDismiss = {
                        dismissJob?.cancel()
                        dismissJob = null
                        current = null
                    },
                    hostPaddingBottom = hostPaddingBottom
                )
            }
        }
    }
}

@SuppressLint("UnnecessaryComposedModifier")
@Composable
private fun BannerLayout(
    spec: NotificationSpec,
    offsetY: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    alpha: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    maxWidthFraction: Float,
    onMeasured: (heightPx: Int) -> Unit,
    onDismiss: () -> Unit,
    hostPaddingBottom: Int = 0
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var measuredHeightPx by remember { mutableStateOf(0) }

    val pointerModifier = Modifier.pointerInput(spec.id, spec.allowSwipeToDismiss) {
        if (!spec.allowSwipeToDismiss) return@pointerInput

        detectVerticalDragGestures(
            onVerticalDrag = { _, dragAmount ->
                val new = offsetY.value + dragAmount
                scope.launch {
                    offsetY.snapTo(new.coerceIn(-1000f, 1000f))
                }
            },
            onDragEnd = {
                scope.launch {
                    val threshold = measuredHeightPx * 0.35f
                    val placedBottom = spec.placement == NotificationPlacement.Bottom

                    if ((placedBottom && offsetY.value > threshold) ||
                        (!placedBottom && offsetY.value < -threshold)
                    ) {
                        val offScreen = if (placedBottom) (measuredHeightPx + 200f) else -(measuredHeightPx + 200f)
                        offsetY.animateTo(
                            targetValue = offScreen,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                        onDismiss()
                    } else {
                        offsetY.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                }
            },
            onDragCancel = {
                scope.launch {
                    offsetY.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
            }
        )
    }

    Surface(
        modifier = Modifier
            .padding(bottom = with(density) { hostPaddingBottom.toDp() })
            .fillMaxWidth(maxWidthFraction)
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .then(pointerModifier)
            .onSizeMeasured { _, h ->
                measuredHeightPx = h
                onMeasured(h)
            }
            .alpha(alpha.value),
        shape = RoundedCornerShape(2.dp),
        color = spec.backgroundColor ?: MaterialTheme.colorScheme.surfaceBright,
        tonalElevation = 12.dp,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            spec.icon?.let {
                Box(modifier = Modifier.padding(end = 16.dp)) {
                    it()
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = spec.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = spec.contentColor
                        ?: MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (spec.actions != null) {
                    Row(
                        modifier = Modifier
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        content = spec.actions
                    )
                }
            }
        }
    }
}

@SuppressLint("SuspiciousModifierThen")
@Composable
private fun Modifier.onSizeMeasured(onSize: (Int, Int) -> Unit): Modifier = composed {
    var measured = remember { false }
    this.then(
        onSizeChanged { size ->
            onSize(size.width, size.height)
            measured = true
        }
    )
}