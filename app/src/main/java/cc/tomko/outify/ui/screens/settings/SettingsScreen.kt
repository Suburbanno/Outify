package cc.tomko.outify.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Interests
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.tomko.outify.ui.components.PreferenceEntry
import cc.tomko.outify.ui.components.PreferenceHeader
import cc.tomko.outify.ui.components.settings.SettingItem
import cc.tomko.outify.ui.components.settings.SettingsGroup
import cc.tomko.outify.ui.viewmodel.settings.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    openInterfaceSettings: (() -> Unit),
    openDebugSettings: (() -> Unit),
) {
    LazyColumn(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            PreferenceHeader("Settings")
            ElevatedCard(
                modifier = modifier
                    .fillMaxWidth()
            ) {
                PreferenceEntry(
                    title = { Text("Interface") },
                    icon = { Icon(Icons.Default.Interests, contentDescription = null) },
                    onClick = openInterfaceSettings,
                )
            }
        }

        item {
            ElevatedCard(
                modifier = modifier
                    .fillMaxWidth()
            ) {
                PreferenceEntry(
                    title = { Text("Misc") },
                    icon = { Icon(Icons.Default.DeveloperMode, contentDescription = null) },
                    onClick = openDebugSettings,
                )
            }
        }
    }
}