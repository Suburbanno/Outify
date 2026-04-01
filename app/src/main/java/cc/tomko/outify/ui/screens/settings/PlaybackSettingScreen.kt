package cc.tomko.outify.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.tomko.outify.ui.components.PreferenceHeader
import cc.tomko.outify.ui.components.SwitchPreferenceEntry
import cc.tomko.outify.data.repository.PlaybackSettings
import cc.tomko.outify.ui.viewmodel.settings.PlaybackSettingViewModel

@Composable
fun PlaybackSettingScreen(
    viewModel: PlaybackSettingViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState(initial = PlaybackSettings())

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
                }
            }
        }
    }
}