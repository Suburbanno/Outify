package cc.tomko.outify.ui.components.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.room.parser.Section

@Composable
fun SettingItem(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Section.Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )

    HorizontalDivider()
}
