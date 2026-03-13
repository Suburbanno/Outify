package cc.tomko.outify.ui.screens.auth

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import cc.tomko.outify.ui.viewmodel.auth.AuthViewModel
import cc.tomko.outify.ui.viewmodel.auth.LibrespotAuthProgress

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val progress by viewModel.progress.collectAsState()

    // Slow floating animation for background shapes
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )
    val rotateAnim by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotate"
    )

    val primaryColor      = MaterialTheme.colorScheme.primary
    val secondaryColor    = MaterialTheme.colorScheme.secondary
    val tertiaryColor     = MaterialTheme.colorScheme.tertiary
    val primaryContainer  = MaterialTheme.colorScheme.primaryContainer

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {

        // background canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val t = floatAnim

            // Bottom-right large blob — primary
            drawExpressiveBlob(
                color    = primaryColor.copy(alpha = 0.13f),
                center   = Offset(w * 0.85f + t * 20f, h * 0.82f - t * 15f),
                radiusX  = w * 0.52f,
                radiusY  = w * 0.45f,
                rotation = rotateAnim + 15f,
            )

            // Top-left blob — secondary
            drawExpressiveBlob(
                color    = secondaryColor.copy(alpha = 0.15f),
                center   = Offset(w * 0.1f - t * 12f, h * 0.15f + t * 10f),
                radiusX  = w * 0.44f,
                radiusY  = w * 0.40f,
                rotation = -rotateAnim - 10f,
            )

            // Mid pill accent — tertiary
            drawSquircle(
                color        = tertiaryColor.copy(alpha = 0.10f),
                center       = Offset(w * 0.5f + t * 8f, h * 0.5f - t * 20f),
                size         = Size(w * 0.7f, w * 0.28f),
                rotation     = rotateAnim * 1.5f,
                cornerRadius = 120f,
            )

            // Small accent circle — primaryContainer
            drawCircle(
                color  = primaryContainer.copy(alpha = 0.25f),
                radius = w * 0.18f,
                center = Offset(w * 0.18f + t * 6f, h * 0.68f - t * 8f),
            )

            // Tiny accent dot — secondary
            drawCircle(
                color  = secondaryColor.copy(alpha = 0.18f),
                radius = w * 0.09f,
                center = Offset(w * 0.78f - t * 5f, h * 0.22f + t * 12f),
            )

            // Large ghost ring — primary
            drawCircle(
                color  = primaryColor.copy(alpha = 0.06f),
                radius = w * 0.72f,
                center = Offset(w * 0.5f, h * 0.5f),
                style  = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
            )
        }

        // Foreground content
        Column(
            verticalArrangement    = Arrangement.Center,
            horizontalAlignment    = Alignment.CenterHorizontally,
            modifier               = Modifier.padding(horizontal = 36.dp),
        ) {
            val emoji = when (progress) {
                LibrespotAuthProgress.START   -> "👋"
                LibrespotAuthProgress.SUCCESS -> "🎉"
                LibrespotAuthProgress.FAILED  -> "⚠"
            }

            AnimatedContent(
                targetState  = emoji,
                transitionSpec = {
                    (scaleIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                            fadeIn()) togetherWith (scaleOut() + fadeOut())
                },
                label = "emoji",
            ) { e ->
                Surface(
                    shape         = RoundedCornerShape(28.dp),
                    color         = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 4.dp,
                    modifier      = Modifier.size(88.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = e, fontSize = 44.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text       = "Welcome to\nOutify",
                style      = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                textAlign  = TextAlign.Center,
                lineHeight = 40.sp,
            )

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedContent(
                targetState    = progress,
                transitionSpec = {
                    (slideInVertically { it / 2 } + fadeIn()) togetherWith
                            (slideOutVertically { -it / 2 } + fadeOut())
                },
                label = "authState",
            ) { state ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier            = Modifier.fillMaxWidth(),
                ) {
                    when (state) {

                        LibrespotAuthProgress.START -> {
                            Text(
                                text      = "Sign in to continue and connect your Spotify account.",
                                style     = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = {
                                    viewModel.startAuth { code, s ->
                                        viewModel.verifyCode(code, s)
                                    }
                                    val url = viewModel.oauthUrl()
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, url.toUri())
                                    )
                                },
                                shape          = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 36.dp, vertical = 16.dp),
                                modifier       = Modifier.fillMaxWidth(0.85f),
                            ) {
                                Text(
                                    "Sign in with Spotify",
                                    style      = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        LibrespotAuthProgress.SUCCESS -> {
                            Text(
                                text      = "You're all set! Credentials saved.",
                                style     = MaterialTheme.typography.bodyLarge,
                                color     = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                            )
                            FilledTonalButton(
                                onClick        = { viewModel.navigateToImport() },
                                shape          = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 36.dp, vertical = 16.dp),
                                modifier       = Modifier.fillMaxWidth(0.85f),
                            ) {
                                Text(
                                    "Continue",
                                    style      = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.AutoMirrored.Filled.NavigateNext,
                                    contentDescription = null,
                                )
                            }
                        }

                        LibrespotAuthProgress.FAILED -> {
                            Surface(
                                shape    = RoundedCornerShape(20.dp),
                                color    = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth(0.9f),
                            ) {
                                Text(
                                    text      = "Authentication failed.\nPlease try again.",
                                    style     = MaterialTheme.typography.bodyMedium,
                                    color     = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center,
                                    modifier  = Modifier.padding(16.dp),
                                )
                            }
                            OutlinedButton(
                                onClick        = { viewModel.restartAuth() },
                                shape          = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 36.dp, vertical = 16.dp),
                                modifier       = Modifier.fillMaxWidth(0.85f),
                            ) {
                                Text(
                                    "Try again",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Draws a softly rotated ellipse to simulate an organic blob.
 */
fun DrawScope.drawExpressiveBlob(
    color: Color,
    center: Offset,
    radiusX: Float,
    radiusY: Float,
    rotation: Float,
) {
    rotate(degrees = rotation, pivot = center) {
        drawOval(
            color    = color,
            topLeft  = Offset(center.x - radiusX, center.y - radiusY),
            size     = Size(radiusX * 2f, radiusY * 2f),
        )
    }
}

/**
 * Draws a pill / squircle shape at [center], rotated by [rotation] degrees.
 */
fun DrawScope.drawSquircle(
    color: Color,
    center: Offset,
    size: Size,
    rotation: Float,
    cornerRadius: Float,
) {
    rotate(degrees = rotation, pivot = center) {
        val path = Path().apply {
            val left   = center.x - size.width  / 2f
            val top    = center.y - size.height / 2f
            val right  = center.x + size.width  / 2f
            val bottom = center.y + size.height / 2f
            val r = cornerRadius.coerceAtMost(size.height / 2f)
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = left, top = top, right = right, bottom = bottom,
                    radiusX = r, radiusY = r,
                )
            )
        }
        drawPath(path, color)
    }
}