package cc.tomko.outify.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import cc.tomko.outify.ui.components.navigation.Route

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier
) {
    Scaffold() { innerPaddings ->
        Column(modifier = Modifier
            .padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding(), start = 24.dp)) {
            Text(
                text = "Welcome back,\nUser!",
                style = MaterialTheme.typography.headlineLargeEmphasized)

            IconButton(
                onClick = {
                    backStack.add(Route.SettingsScreen)
                }
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        }
    }
}

@Composable
fun SharedTransitionScope.HomePlaylistView(
    modifier: Modifier = Modifier,
) {

}