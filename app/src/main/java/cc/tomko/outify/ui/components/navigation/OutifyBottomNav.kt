package cc.tomko.outify.ui.components.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

data class NavDestination(
    val id: String,
    val label: String,
    val route: Route,
    val icon: @Composable () -> Unit,
)

@Composable
fun OutifyBottomNav(
    items: List<NavDestination>,
    selectedId: String?,
    onItemSelected: (NavDestination) -> Unit,
    modifier: Modifier = Modifier,
    tonalElevation: Dp = NavigationBarDefaults.Elevation,
    showLabels: Boolean = true,
){
    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = tonalElevation,
        windowInsets = NavigationBarDefaults.windowInsets,
    ) {
        items.forEach { item ->
            NavigationBarItem(
                selected = item.id == selectedId,
                onClick = { onItemSelected(item) },
                icon = item.icon,
                label = { Text(item.label) },
                alwaysShowLabel = showLabels,
            )
        }
    }
}
