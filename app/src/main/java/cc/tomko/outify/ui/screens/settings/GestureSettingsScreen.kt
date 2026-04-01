package cc.tomko.outify.ui.screens.settings

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.tomko.outify.core.model.Track
import cc.tomko.outify.data.setting.GestureSetting
import cc.tomko.outify.data.setting.GestureTrigger
import cc.tomko.outify.data.setting.Side
import cc.tomko.outify.ui.components.PreferenceEntry
import cc.tomko.outify.ui.components.PreferenceHeader
import cc.tomko.outify.ui.components.SwitchPreferenceEntry
import cc.tomko.outify.ui.components.bottomsheet.GestureCustomizeBottomSheet
import cc.tomko.outify.ui.components.rows.SwipeableTrackRowConfigured
import cc.tomko.outify.ui.viewmodel.settings.GestureSettingViewModel

@Composable
fun SharedTransitionScope.GestureSettingsScreen(
    viewModel: GestureSettingViewModel,
    modifier: Modifier = Modifier
) {
    val gestures by viewModel.gestures.collectAsState()
    val swipeEnabled by viewModel.swipeEnabled.collectAsState()
    val scope = rememberCoroutineScope()

    var customizeGesture by remember { mutableStateOf<GestureSetting?>(null)}
    var customizeGestureIndex by remember { mutableStateOf<Int?>(null)}

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            PreferenceHeader("Gestures")

            ElevatedCard(
                modifier = modifier
                    .fillMaxWidth()
            ) {
                SwitchPreferenceEntry(
                    title = { Text("Enable swipe gestures") },
                    description = "Quick action on track row",
                    icon = { Icon(Icons.Default.Gesture, contentDescription = null) },
                    onCheckedChange = { viewModel.setGesturesEnabled(it) },
                    isChecked = swipeEnabled
                )
            }
        }

        // gestures list
        itemsIndexed(gestures) { index, gesture ->
            val gestureType = when (gesture.trigger) {
                GestureTrigger.LongPress -> "long press"
                else -> "swipe gesture"
            }

            val direction = when (gesture.side) {
                Side.End -> "LTR"
                Side.Start -> "RTL"
                else -> null
            }

            val description = buildString {
                append("On trigger: ${gesture.action.name}")
                if (gesture.trigger != GestureTrigger.LongPress) {
                    append(" • Direction: $direction")
                }
            }

            PreferenceEntry(
                title = { Text("Customize $gestureType") },
                description = description,
                icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = {
                    customizeGesture = gesture
                    customizeGestureIndex = index
                },
                trailingContent = {
                    IconButton(
                        onClick = {
                            viewModel.removeGesture(index)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }

        if(swipeEnabled) {
            item {
                ElevatedCard(
                    modifier = modifier
                        .fillMaxWidth()
                ) {
                    PreferenceEntry(
                        title = { Text("Add gesture") },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        onClick = {
                            viewModel.addGesture()
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Try it out"
            )

            SwipeableTrackRowConfigured(
                track = Track.dummy()
            )
        }
    }

    if (customizeGesture != null){
        GestureCustomizeBottomSheet(
            gesture = customizeGesture!!,
            onDismiss = {
                // Saving
                viewModel.updateGestureAt(customizeGestureIndex!!, it)
                customizeGesture = null
                customizeGestureIndex = null
            },
        )
    }
}