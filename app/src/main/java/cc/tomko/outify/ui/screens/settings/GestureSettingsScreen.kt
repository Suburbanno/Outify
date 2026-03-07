package cc.tomko.outify.ui.screens.settings

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cc.tomko.outify.data.setting.GestureAction
import cc.tomko.outify.data.setting.GestureSetting
import cc.tomko.outify.data.setting.GestureTrigger
import cc.tomko.outify.data.setting.Side
import cc.tomko.outify.ui.components.settings.SettingsGroup
import cc.tomko.outify.ui.components.settings.SettingItem
import cc.tomko.outify.ui.viewmodel.settings.GestureSettingViewModel
import kotlinx.coroutines.launch

@Composable
fun GestureSettingsScreen(
    viewModel: GestureSettingViewModel,
    modifier: Modifier = Modifier
) {
    val gestures by viewModel.gestures.collectAsState()
    val swipeEnabled by viewModel.swipeEnabled.collectAsState()
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            // Settings header group
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    ListItem(
                        headlineContent = { Text("Enable swipe gestures") },
                        supportingContent = { Text("Quick actions on track rows") },
                        trailingContent = {
                            Switch(checked = swipeEnabled, onCheckedChange = { viewModel.setGesturesEnabled(it) })
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Button(onClick = { viewModel.addGesture() }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Add gesture")
                        }

                        Button(onClick = {
                            scope.launch { viewModel.saveGestures(gestures) }
                        }) {
                            Text("Save")
                        }
                    }
                }
            }
        }

        // gestures list
        itemsIndexed(gestures) { index, gesture ->
            GestureEditorCard(
                gesture = gesture,
                index = index,
                onToggleEnabled = { viewModel.updateGestureAt(index, gesture.copy(enabled = it)) },
                onActionSelected = { action -> viewModel.updateGestureAt(index, gesture.copy(action = action)) },
                onSideSelected = { side -> viewModel.updateGestureAt(index, gesture.copy(side = side)) },
                onThresholdChanged = { threshold -> viewModel.updateGestureAt(index, gesture.copy(thresholdFraction = threshold)) },
                onColorPicked = { hex -> viewModel.updateGestureAt(index, gesture.copy(backgroundHex = hex)) },
                onMoveUp = { viewModel.moveUp(index) },
                onMoveDown = { viewModel.moveDown(index) },
                onRemove = { viewModel.removeGesture(index) },
                onTriggerSelected = { newTrigger -> viewModel.updateGestureAt(index, gesture.copy(trigger = newTrigger)) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("DefaultLocale")
@Composable
private fun GestureEditorCard(
    gesture: GestureSetting,
    index: Int,
    onToggleEnabled: (Boolean) -> Unit,
    onActionSelected: (GestureAction) -> Unit,
    onSideSelected: (Side) -> Unit,
    onThresholdChanged: (Float) -> Unit,
    onTriggerSelected: (GestureTrigger) -> Unit,
    onColorPicked: (Long?) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var actionMenuExpanded by remember { mutableStateOf(false) }
    val actions = GestureAction.entries

    Card(modifier = Modifier
        .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(text = gesture.action.name, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(checked = gesture.enabled, onCheckedChange = onToggleEnabled)
                        IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = "Remove") }
                    }
                    Text(
                        text = "Side: ${gesture.side.name} • Threshold: ${percent(gesture.thresholdFraction ?: 0.25f)}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))

                // ACTION picker (simple dropdown)
                ExposedDropdownMenuBox(
                    expanded = actionMenuExpanded,
                    onExpandedChange = { actionMenuExpanded = it }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { actionMenuExpanded = true }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(gesture.action.name)
                        Spacer(modifier = Modifier.weight(1f))
                        TrailingIcon(expanded = actionMenuExpanded)
                    }

                    androidx.compose.material3.DropdownMenu(
                        expanded = actionMenuExpanded,
                        onDismissRequest = { actionMenuExpanded = false }
                    ) {
                        actions.forEach { act ->
                            androidx.compose.material3.DropdownMenuItem(text = { Text(act.name) }, onClick = {
                                onActionSelected(act)
                                actionMenuExpanded = false
                            })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // TRIGGER selector
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Trigger:")
                    Spacer(modifier = Modifier.size(8.dp))
                    val triggers = listOf(GestureTrigger.SwipeStart, GestureTrigger.SwipeEnd, GestureTrigger.LongPress)
                    triggers.forEach { t ->
                        Row(
                            modifier = Modifier
                                .clickable {
                                    onTriggerSelected(t)
                                }
                                .padding(end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = gesture.trigger == t, onClick = {
                                onTriggerSelected(t)
                            })
                            Text(t.name, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // SIDE selector
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Side:")
                    Spacer(modifier = Modifier.size(8.dp))
                    listOf(Side.Start, Side.End).forEach { s ->
                        Row(modifier = Modifier
                            .clickable { onSideSelected(s) }
                            .padding(end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = gesture.side == s, onClick = { onSideSelected(s) })
                            Text(s.name, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // THRESHOLD slider
                Column {
                    Text("Threshold: ${percent(gesture.thresholdFraction ?: 0.25f)}")
                    var sliderVal by rememberSaveable { mutableStateOf(gesture.thresholdFraction ?: 0.25f) }
                    Slider(
                        value = sliderVal,
                        onValueChange = { sliderVal = it },
                        valueRange = 0.02f..0.8f,
                        onValueChangeFinished = { onThresholdChanged(sliderVal) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // COLOR swatches
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Background:")
                    Spacer(modifier = Modifier.size(8.dp))
                    val colors: List<Long?> = listOf(
                        0xFF3C8C52L,
                        0xFFB00020L,
                        0xFF0066CCL,
                        null
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colors.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(if (hex != null) Color(hex) else Color.Transparent)
                                    .clickable { onColorPicked(hex) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // reorder controls
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onMoveUp) { Icon(Icons.Default.ArrowDropUp, contentDescription = "Move up") }
                    IconButton(onClick = onMoveDown) { Icon(Icons.Default.ArrowDropDown, contentDescription = "Move down") }
                    Spacer(modifier = Modifier.weight(1f))
                    // quick preview hint
                    Text("Preview by swiping a row in the lists", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
private fun percent(f: Float): String = String.format("%.0f%%", (f.coerceIn(0f, 1f) * 100f))