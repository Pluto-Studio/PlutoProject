package plutoproject.feature.menu.api.paper

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

val LocalMenuScreenModel: ProvidableCompositionLocal<MenuScreenModel> =
    staticCompositionLocalOf { error("Unexpected") }
