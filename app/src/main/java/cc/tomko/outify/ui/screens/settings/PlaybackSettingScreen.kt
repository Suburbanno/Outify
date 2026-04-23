package cc.tomko.outify.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.tomko.outify.ui.components.PreferenceHeader
import cc.tomko.outify.ui.components.SwitchPreferenceEntry
import cc.tomko.outify.data.repository.PlaybackSettings
import cc.tomko.outify.playback.model.Bitrate
import cc.tomko.outify.ui.components.DropdownOption
import cc.tomko.outify.ui.components.DropdownPreferenceEntry
import cc.tomko.outify.ui.components.PreferenceEntry
import cc.tomko.outify.ui.viewmodel.settings.PlaybackSettingViewModel
import kotlin.collections.listOf

@Composable
fun PlaybackSettingScreen(
    viewModel: PlaybackSettingViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState(initial = PlaybackSettings())
    val restartNeeded by viewModel.needsRestart.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize()
            .padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            PreferenceHeader("Playback")

            ElevatedCard(
                modifier = modifier
                    .fillMaxWidth()
            ) {
                Column {
                    SwitchPreferenceEntry(
                        title = { Text("Gapless playback") },
                        description = "Smooth playback without gaps",
                        icon = { Icon(Icons.Default.SkipNext, contentDescription = null) },
                        onCheckedChange = { viewModel.setGaplessPlayback(it) },
                        isChecked = settings.gapless
                    )

                    SwitchPreferenceEntry(
                        title = { Text("Normalize audio") },
                        description = "Every track will be the same loudness",
                        icon = {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeDown,
                                contentDescription = null
                            )
                        },
                        onCheckedChange = { viewModel.setNormalizeAudio(it) },
                        isChecked = settings.normalizeAudio
                    )

                    SwitchPreferenceEntry(
                        title = { Text("Keepalive") },
                        description = "Allow resurrection from notification",
                        icon = {
                            Icon(
                                Icons.Default.Healing,
                                contentDescription = null
                            )
                        },
                        onCheckedChange = { viewModel.setKeepAlive(it) },
                        isChecked = settings.keepalive
                    )

                    DropdownPreferenceEntry(
                        title = { Text("Bitrate (Quality)") },
                        description = "Choose your preferred streaming quality",
                        icon = { Icon(Icons.Default.HighQuality, contentDescription = null ) },
                        options = listOf(
                            DropdownOption(Bitrate.KBPS320, "320Kbps, ${Bitrate.KBPS320.name}"),
                            DropdownOption(Bitrate.KBPS160, "160Kbps, ${Bitrate.KBPS160.name}"),
                            DropdownOption(Bitrate.KBPS96, "96Kbps, ${Bitrate.KBPS96.name}"),
                        ),
                        selectedValue = settings.bitrate,
                        onValueChange = { viewModel.setBitrate(it) }
                    )
                }
            }
        }

        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (restartNeeded)
                        MaterialTheme.colorScheme.tertiaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                ),
                modifier =  Modifier
                    .fillMaxWidth()
            ) {
                Column {
                    PreferenceEntry(
                        title = { Text("Restart Spirc") },
                        description = "Required to apply playback related settings",
                        icon = { Icon(Icons.Default.RestartAlt, contentDescription = null) },
                        onClick = {
                            viewModel.restartSpirc()
                        },
                        trailingContent = {
                            AnimatedVisibility(
                                visible = restartNeeded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                ) {
                                    Text("!")
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}