package cc.tomko.outify.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Interests
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.tomko.outify.ui.components.PreferenceEntry
import cc.tomko.outify.ui.components.PreferenceHeader
import cc.tomko.outify.ui.components.settings.SettingItem
import cc.tomko.outify.ui.components.settings.SettingsGroup
import cc.tomko.outify.ui.components.settings.SwitchSettingItem
import cc.tomko.outify.ui.repository.InterfaceSettings
import cc.tomko.outify.ui.viewmodel.settings.InterfaceViewModel

@Composable
fun InterfaceSettingScreen(
    viewModel: InterfaceViewModel,
    openGestureSettings: (() -> Unit),
    openAppearanceSettings: (() -> Unit),
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            PreferenceHeader("Interface")

            ElevatedCard(
                modifier = modifier
                    .fillMaxWidth()
            ) {
                PreferenceEntry(
                    title = { Text("Gestures") },
                    description = "Personalize gestures",
                    icon = { Icon(Icons.Default.Gesture, contentDescription = null) },
                    onClick = openGestureSettings,
                )
            }
        }

        item {
            ElevatedCard(
                modifier = modifier
                    .fillMaxWidth()
            ) {
                PreferenceEntry(
                    title = { Text("Appearance") },
                    description = "Customize the design",
                    icon = { Icon(Icons.Default.DesignServices, contentDescription = null) },
                    onClick = openAppearanceSettings,
                )
            }
        }
    }
}