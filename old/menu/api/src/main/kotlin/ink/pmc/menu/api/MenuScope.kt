package ink.pmc.menu.api

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

val LocalMenuScreenModel: ProvidableCompositionLocal<MenuScreenModel> =
    staticCompositionLocalOf { error("Unexpected") }