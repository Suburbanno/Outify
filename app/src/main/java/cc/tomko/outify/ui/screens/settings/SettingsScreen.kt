package cc.tomko.outify.ui.screens.settings

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    ) {
        item {
            SettingsGroup("Visual"){
                SettingItem(
                    title = "Interface",
                    subtitle = "Modify the interface",
                    onClick = openInterfaceSettings
                )
            }
        }

        item {
            SettingsGroup("Misc") {
                SettingItem(
                    title = "Debug",
                    subtitle = "Developer tools",
                    onClick = openDebugSettings
                )
            }
        }
    }
}