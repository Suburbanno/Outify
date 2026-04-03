package cc.tomko.outify.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cc.tomko.outify.ui.components.PreferenceEntry
import cc.tomko.outify.ui.components.PreferenceHeader
import cc.tomko.outify.ui.viewmodel.settings.AccountsViewModel
import cc.tomko.outify.ui.viewmodel.settings.InterfaceViewModel

@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            PreferenceHeader("Accounts")

            ElevatedCard(
                modifier = modifier
                    .fillMaxWidth()
            ) {
                PreferenceEntry(
                    title = { Text("Playback login") },
                    icon = { Icon(Icons.Default.Done, contentDescription = null) },
                    onClick = { },
                )

                PreferenceEntry(
                    title = { Text("Account login") },
                    icon = { Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null) },
                    onClick = { viewModel.startAccountAuth(context) },
                )
            }
        }
    }
}