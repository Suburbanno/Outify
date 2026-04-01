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
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.MonochromePhotos
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cc.tomko.outify.ui.components.PreferenceHeader
import cc.tomko.outify.ui.components.PreferenceSectionHeader
import cc.tomko.outify.ui.components.SwitchPreferenceEntry
import cc.tomko.outify.data.repository.InterfaceSettings
import cc.tomko.outify.ui.viewmodel.settings.AppearanceViewModel

@Composable
fun AppearanceSettingScreen(
    viewModel: AppearanceViewModel,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle(initialValue = InterfaceSettings())

    LazyColumn(
        modifier = Modifier.fillMaxSize()
            .padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            PreferenceHeader("Appearance")

            ElevatedCard(
                modifier = modifier
                    .fillMaxWidth(),
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
        item {
            if(settings.monochromeImages) {
                PreferenceSectionHeader("Monochrome settings")

                ElevatedCard {
                    SwitchPreferenceEntry(
                        title = { Text("Monochrome albums") },
                        description = "Album artwork in album views will be monochrome",
                        icon = { Icon(Icons.Default.Album, contentDescription = null) },
                        isChecked = settings.monochromeAlbums,
                        onCheckedChange = { enabled ->
                            viewModel.setMonochromeAlbums(enabled)
                        }
                    )

                    SwitchPreferenceEntry(
                        title = { Text("Monochrome artists") },
                        description = "Artist artwork in artist views will be monochrome",
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        isChecked = settings.monochromeArtists,
                        onCheckedChange = { enabled ->
                            viewModel.setMonochromeArtists(enabled)
                        }
                    )

                    SwitchPreferenceEntry(
                        title = { Text("Monochrome playlists") },
                        description = "Playlist artwork will be monochrome",
                        icon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null) },
                        isChecked = settings.monochromePlaylists,
                        onCheckedChange = { enabled ->
                            viewModel.setMonochromePlaylists(enabled)
                        }
                    )

                    SwitchPreferenceEntry(
                        title = { Text("Monochrome tracks") },
                        description = "Track rows will be monochrome",
                        icon = { Icon(Icons.Default.Audiotrack, contentDescription = null) },
                        isChecked = settings.monochromeTracks,
                        onCheckedChange = { enabled ->
                            viewModel.setMonochromeTracks(enabled)
                        }
                    )

                    SwitchPreferenceEntry(
                        title = { Text("Monochrome player") },
                        description = "Player (mini & fullscreen) will be monochrome",
                        icon = { Icon(Icons.Default.PlayCircleOutline, contentDescription = null) },
                        isChecked = settings.monochromePlayer,
                        onCheckedChange = { enabled ->
                            viewModel.setMonochromePlayer(enabled)
                        }
                    )

                    SwitchPreferenceEntry(
                        title = { Text("Monochrome headers") },
                        description = "Page headers will be monochrome",
                        icon = { Icon(Icons.Default.Topic, contentDescription = null) },
                        isChecked = settings.monochromeHeaders,
                        onCheckedChange = { enabled ->
                            viewModel.setMonochromeHeaders(enabled)
                        }
                    )
                }
            }
        }

        item {
            PreferenceSectionHeader("Dynamic")

            ElevatedCard {
                SwitchPreferenceEntry(
                    title = { Text("Dynamic theme") },
                    description = "Colorscheme will change according to current track",
                    icon = { Icon(Icons.Default.DesignServices, contentDescription = null) },
                    isChecked = settings.dynamicTheme,
                    onCheckedChange = { enabled ->
                        viewModel.setDynamicTheme(enabled)
                    }
                )

                SwitchPreferenceEntry(
                    title = { Text("Pure black") },
                    description = "Use AMOLED black",
                    icon = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                    isChecked = settings.pureBlack,
                    onCheckedChange = { enabled ->
                        viewModel.setPureBlack(enabled)
                    }
                )

                SwitchPreferenceEntry(
                    title = { Text("High contrast") },
                    icon = { Icon(Icons.Default.Contrast, contentDescription = null) },
                    isChecked = settings.highContrastCompat,
                    onCheckedChange = { enabled ->
                        viewModel.setHighContrastCompat(enabled)
                    }
                )
            }
        }
    }
}