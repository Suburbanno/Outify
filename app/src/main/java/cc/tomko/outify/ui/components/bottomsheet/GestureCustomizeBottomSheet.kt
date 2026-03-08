package cc.tomko.outify.ui.components.bottomsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.tomko.outify.data.setting.GestureAction
import cc.tomko.outify.data.setting.GestureSetting
import cc.tomko.outify.data.setting.GestureTrigger
import cc.tomko.outify.data.setting.Side

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureCustomizeBottomSheet(
    gesture: GestureSetting,
    onDismiss: (GestureSetting) -> Unit,
    modifier: Modifier = Modifier,
) {
    var enabledState by remember { mutableStateOf(gesture.enabled) }
    var thresholdValue by remember { mutableFloatStateOf(gesture.thresholdFraction ?: 0.25f) }
    var triggerValue by remember { mutableStateOf(gesture.trigger) }
    var sideValue by remember { mutableStateOf(gesture.side) }
    var actionValue by remember { mutableStateOf(gesture.action) }
    var actionExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = {
            onDismiss(GestureSetting(
                action = actionValue,
                side = if(triggerValue == GestureTrigger.LongPress) null else sideValue,
                trigger = triggerValue,
                enabled = enabledState,
                thresholdFraction = if(triggerValue == GestureTrigger.LongPress) null else thresholdValue,
            ))
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Customize gesture",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )

                Switch(
                    checked = enabledState,
                    onCheckedChange = {
                        enabledState = it
                    },
                )
            }

            if (triggerValue != GestureTrigger.LongPress) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {

                    Slider(
                        value = thresholdValue,
                        onValueChange = { thresholdValue = it },
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "${(thresholdValue * 100).toInt()}%",
                    )
                }
            }

            // Gesture trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Trigger on",
                    modifier = Modifier.weight(1f)
                )

                GestureTrigger.entries.forEach {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = triggerValue == it,
                            onClick = { triggerValue = it },
                        )

                        Text(
                            text = it.name.replace("_", " ")
                        )
                    }
                }
            }

            // Side picker
            if (triggerValue != GestureTrigger.LongPress) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Swipe direction",
                        modifier = Modifier.weight(1f)
                    )

                    Side.entries.forEach {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = sideValue == it,
                                onClick = { sideValue = it },
                            )

                            Text(
                                text = if (it == Side.End) "Left to right" else "Right to left"
                            )
                        }
                    }
                }
            }

            // Action picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = actionExpanded,
                    onExpandedChange = { actionExpanded = !actionExpanded }
                ) {

                    OutlinedTextField(
                        value = actionValue.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Action") },
                        trailingIcon = {
                            TrailingIcon(actionExpanded)
                        },
                        modifier = Modifier.menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = actionExpanded,
                        onDismissRequest = { actionExpanded = false }
                    ) {

                        GestureAction.entries.forEach { action ->
                            DropdownMenuItem(
                                text = { Text(action.name) },
                                onClick = {
                                    actionValue = action
                                    actionExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}
