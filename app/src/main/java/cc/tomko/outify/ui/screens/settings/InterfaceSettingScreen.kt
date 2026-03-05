package cc.tomko.outify.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cc.tomko.outify.ui.components.settings.SettingItem
import cc.tomko.outify.ui.components.settings.SettingsGroup
import cc.tomko.outify.ui.components.settings.SwitchSettingItem
import cc.tomko.outify.ui.repository.InterfaceSettings
import cc.tomko.outify.ui.viewmodel.settings.InterfaceViewModel

@Composable
fun InterfaceSettingScreen(
    viewModel: InterfaceViewModel,
    openGestureSettings: (() -> Unit),
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState(initial = InterfaceSettings())

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            SettingsGroup("Gestures") {
                SwitchSettingItem(
                    title = "Enable swipe gestures",
                    subtitle = "Swipe tracks for quick actions",
                    checked = settings.swipeGesturesEnabled,
                    onCheckedChange = viewModel::setSwipeGesturesEnabled,
                )

                SettingItem(
                    title = "Edit swipe gestures",
                    subtitle = "Configure left and right swipe gestures",
                    onClick = openGestureSettings
                )
            }
        }
    }
}