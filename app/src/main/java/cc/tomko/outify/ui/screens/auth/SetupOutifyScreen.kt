package cc.tomko.outify.ui.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.tomko.outify.ui.viewmodel.auth.LoadingTask
import cc.tomko.outify.ui.viewmodel.auth.SetupConfig
import cc.tomko.outify.ui.viewmodel.auth.SetupStep
import cc.tomko.outify.ui.viewmodel.auth.SetupViewModel
import cc.tomko.outify.ui.viewmodel.auth.StreamingQuality
import cc.tomko.outify.ui.viewmodel.auth.TaskState

@Composable
fun SetupOutifyScreen(
    viewModel: SetupViewModel,
    modifier: Modifier = Modifier,
) {
    val step     by viewModel.step.collectAsState()
    val config   by viewModel.config.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val tasks    by viewModel.tasks.collectAsState()

    // ── Shared blob animation ─────────────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(9000, easing = LinearEasing), RepeatMode.Reverse,
        ), label = "float",
    )
    val rotateAnim by infiniteTransition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(
            tween(11000, easing = FastOutSlowInEasing), RepeatMode.Reverse,
        ), label = "rotate",
    )

    val primaryColor     = MaterialTheme.colorScheme.primary
    val secondaryColor   = MaterialTheme.colorScheme.secondary
    val tertiaryColor    = MaterialTheme.colorScheme.tertiary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    Box(modifier = modifier.fillMaxSize()) {

        // ── Background blobs ──────────────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val t = floatAnim

            // Large primary blob — top-right
            drawExpressiveBlob(
                color    = primaryColor.copy(alpha = 0.11f),
                center   = Offset(w * 0.88f - t * 14f, h * 0.18f + t * 18f),
                radiusX  = w * 0.50f,
                radiusY  = w * 0.42f,
                rotation = rotateAnim + 20f,
            )
            // Secondary blob — bottom-left
            drawExpressiveBlob(
                color    = secondaryColor.copy(alpha = 0.14f),
                center   = Offset(w * 0.12f + t * 10f, h * 0.85f - t * 12f),
                radiusX  = w * 0.46f,
                radiusY  = w * 0.38f,
                rotation = -rotateAnim - 12f,
            )
            // Tertiary pill — diagonal centre
            drawSquircle(
                color        = tertiaryColor.copy(alpha = 0.09f),
                center       = Offset(w * 0.48f + t * 10f, h * 0.52f - t * 16f),
                size         = Size(w * 0.65f, w * 0.26f),
                rotation     = rotateAnim * 1.8f + 35f,
                cornerRadius = 120f,
            )
            // Small accent dot — secondary
            drawCircle(
                color  = secondaryColor.copy(alpha = 0.16f),
                radius = w * 0.10f,
                center = Offset(w * 0.82f + t * 6f, h * 0.74f - t * 8f),
            )
            // Accent circle — primaryContainer
            drawCircle(
                color  = primaryContainer.copy(alpha = 0.22f),
                radius = w * 0.16f,
                center = Offset(w * 0.22f - t * 5f, h * 0.32f + t * 10f),
            )
            // Ghost ring
            drawCircle(
                color  = primaryColor.copy(alpha = 0.05f),
                radius = w * 0.68f,
                center = Offset(w * 0.5f, h * 0.5f),
                style  = Stroke(width = 1.5.dp.toPx()),
            )
        }

        StepDots(
            currentStep = step,
            modifier    = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 52.dp),
        )

        AnimatedContent(
            targetState    = step,
            transitionSpec = {
                (slideInVertically { it / 3 } + fadeIn(tween(380))) togetherWith
                        (slideOutVertically { -it / 3 } + fadeOut(tween(280)))
            },
            label          = "setupStep",
            modifier       = Modifier.fillMaxSize(),
        ) { currentStep ->
            when (currentStep) {
                SetupStep.CONFIG -> ConfigStep(
                    config          = config,
                    onQuality       = viewModel::setQuality,
                    onGaplessPlayback = viewModel::toggleGaplessPlayback,
                    onContinue      = viewModel::startLoading,
                )
                SetupStep.LOADING -> LoadingStep(
                    progress = progress,
                    tasks    = tasks,
                )
                SetupStep.DONE -> DoneStep(
                    onNavigate = viewModel::navigateToMain,
                )
            }
        }
    }
}

@Composable
private fun ConfigStep(
    config: SetupConfig,
    onQuality: (StreamingQuality) -> Unit,
    onGaplessPlayback: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp)
            .padding(top = 112.dp, bottom = 36.dp),
        verticalArrangement    = Arrangement.spacedBy(28.dp),
        horizontalAlignment    = Alignment.CenterHorizontally,
    ) {
        // Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape          = RoundedCornerShape(24.dp),
                color          = MaterialTheme.colorScheme.tertiaryContainer,
                tonalElevation = 2.dp,
                modifier       = Modifier.size(72.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("⚡", fontSize = 36.sp)
                }
            }
            Text(
                text       = "Quick Setup",
                style      = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign  = TextAlign.Center,
            )
            Text(
                text      = "A few choices to get Outify\nfeeling just right for you.",
                style     = MaterialTheme.typography.bodyLarge,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        // Streaming quality
        ConfigSection(
            icon  = { Icon(Icons.Default.GraphicEq, contentDescription = null) },
            label = "Streaming Quality",
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier              = Modifier.fillMaxWidth(),
            ) {
                StreamingQuality.entries.forEach { quality ->
                    FilterChip(
                        selected = config.streamingQuality == quality,
                        onClick  = { onQuality(quality) },
                        label    = { Text(quality.label, fontWeight = FontWeight.Medium) },
                        shape    = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f),
                        leadingIcon = if (config.streamingQuality == quality) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                    )
                }
            }
        }

        // Gapless toggle
        ConfigSection(
            icon  = { Icon(Icons.Default.SkipNext, contentDescription = null) },
            label = "Gapless playback",
        ) {
            ConfigToggleRow(
                title    = "Enable gapless playback",
                subtitle = "Smoothly blend between songs",
                checked  = config.enableGaplessPlayback,
                onToggle = onGaplessPlayback,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick        = onContinue,
            shape          = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 40.dp, vertical = 18.dp),
            modifier       = Modifier.fillMaxWidth(0.88f),
        ) {
            Text(
                "Let's go",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null)
        }
    }
}

@Composable
private fun ConfigSection(
    icon: @Composable () -> Unit,
    label: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape          = RoundedCornerShape(24.dp),
        color          = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        modifier       = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                    icon()
                }
                Text(
                    text       = label,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface,
                )
            }
            content()
        }
    }
}

@Composable
private fun ConfigToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier              = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

// ── Step: LOADING ─────────────────────────────────────────────────────────────

@Composable
private fun LoadingStep(
    progress: Float,
    tasks: List<LoadingTask>,
) {
    val animatedProgress by animateFloatAsState(
        targetValue    = progress,
        animationSpec  = tween(600, easing = FastOutSlowInEasing),
        label          = "progress",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        verticalArrangement    = Arrangement.Center,
        horizontalAlignment    = Alignment.CenterHorizontally,
    ) {
        // Big circular progress
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress       = { animatedProgress },
                modifier       = Modifier.size(148.dp),
                strokeWidth    = 8.dp,
                trackColor     = MaterialTheme.colorScheme.surfaceContainerHighest,
                color          = MaterialTheme.colorScheme.primary,
                strokeCap      = StrokeCap.Round,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text       = "${(animatedProgress * 100).toInt()}%",
                    style      = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text  = "syncing",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text       = "Getting everything ready",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            textAlign  = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text      = "This only takes a moment.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Task list
        Surface(
            shape          = RoundedCornerShape(28.dp),
            color          = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier       = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier            = Modifier.padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                tasks.forEach { task ->
                    TaskRow(task = task)
                }
            }
        }
    }
}

@Composable
private fun TaskRow(task: LoadingTask) {
    val isRunning = task.state == TaskState.RUNNING
    val isDone    = task.state == TaskState.DONE
    val isFailed = task.state == TaskState.FAILED

    val rowAlpha by animateFloatAsState(
        targetValue   = if (task.state == TaskState.PENDING) 0.4f else 1f,
        animationSpec = tween(300),
        label         = "rowAlpha_${task.id}",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_${task.id}")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label         = "pulse",
    )

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .graphicsLayer { alpha = rowAlpha },
    ) {
        // State icon / spinner
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier.size(32.dp),
        ) {
            when {
                isDone    -> Surface(
                    shape  = RoundedCornerShape(50),
                    color  = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(28.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                isRunning -> CircularProgressIndicator(
                    modifier    = Modifier
                        .size(24.dp)
                        .graphicsLayer { alpha = pulse },
                    strokeWidth = 2.5.dp,
                    color       = MaterialTheme.colorScheme.secondary,
                )
                else      -> Text(task.icon, fontSize = 20.sp)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = task.label,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isRunning) FontWeight.SemiBold else FontWeight.Normal,
                color      = if (isRunning) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AnimatedVisibility(
            visible = isDone,
            enter   = scaleIn(spring(stiffness = Spring.StiffnessMedium)) + fadeIn(),
            exit    = scaleOut() + fadeOut(),
        ) {
            Text(
                text  = "Done",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }

        AnimatedVisibility(
            visible = isFailed,
            enter   = scaleIn(spring(stiffness = Spring.StiffnessMedium)) + fadeIn(),
            exit    = scaleOut() + fadeOut(),
        ) {
            Text(
                text  = "Failed",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun DoneStep(onNavigate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        verticalArrangement    = Arrangement.Center,
        horizontalAlignment    = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape          = RoundedCornerShape(32.dp),
            color          = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 4.dp,
            modifier       = Modifier.size(100.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("🎧", fontSize = 52.sp)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text       = "You're all\nset up!",
            style      = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            textAlign  = TextAlign.Center,
            lineHeight = 40.sp,
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text      = "Everything is loaded and ready.\nEnjoy the music.",
            style     = MaterialTheme.typography.bodyLarge,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick        = onNavigate,
            shape          = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 18.dp),
            modifier       = Modifier.fillMaxWidth(0.88f),
        ) {
            Text(
                "Start listening",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null)
        }
    }
}

@Composable
private fun StepDots(currentStep: SetupStep, modifier: Modifier = Modifier) {
    val steps  = SetupStep.entries
    val colors = MaterialTheme.colorScheme

    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        steps.forEach { step ->
            val isActive  = step == currentStep
            val isPast    = step.ordinal < currentStep.ordinal
            val dotWidth  by animateDpAsState(
                targetValue   = if (isActive) 28.dp else 8.dp,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label         = "dot_${step.name}",
            )
            val dotColor  = when {
                isActive -> colors.primary
                isPast   -> colors.primaryContainer
                else     -> colors.outlineVariant
            }
            Surface(
                shape    = RoundedCornerShape(50),
                color    = dotColor,
                modifier = Modifier
                    .height(8.dp)
                    .width(dotWidth),
            ) {}
        }
    }
}