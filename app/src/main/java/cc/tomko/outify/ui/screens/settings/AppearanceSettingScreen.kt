package cc.tomko.outify.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.MonochromePhotos
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cc.tomko.outify.ui.components.PreferenceEntry
import cc.tomko.outify.ui.components.SwitchPreferenceEntry
import cc.tomko.outify.ui.repository.InterfaceSettings
import cc.tomko.outify.ui.viewmodel.settings.AppearanceViewModel

@Composable
fun AppearanceSettingScreen(
    viewModel: AppearanceViewModel,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle(initialValue = InterfaceSettings())

    LazyColumn(
        modifier = Modifier.fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ElevatedCard(
                modifier = modifier
                    .fillMaxWidth()
            ) {
                SwitchPreferenceEntry(
                    title = { Text("Monochrome artwork") },
                    description = "Every image will be black & white",
                    icon = { Icon(Icons.Default.MonochromePhotos, contentDescription = null) },
                    isChecked = settings.monochromeImages,
                    onCheckedChange = { enabled ->
                        viewModel.setMonochromeImages(enabled)
                    }
                )
            }
        }
    }
}