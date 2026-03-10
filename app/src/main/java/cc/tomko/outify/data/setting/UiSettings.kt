package cc.tomko.outify.data.setting

import androidx.compose.runtime.compositionLocalOf
import cc.tomko.outify.ui.components.navigation.Route
import cc.tomko.outify.ui.repository.InterfaceSettings

val LocalUiSettings = compositionLocalOf {
    InterfaceSettings()
}