package ink.pmc.menu.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import ink.pmc.menu.api.MenuPrebuilt
import ink.pmc.menu.api.MenuScreenModel

class MenuV2ScreenModel : ScreenModel, MenuScreenModel {
    override var currentPageId by mutableStateOf(MenuPrebuilt.Pages.HOME_PAGE_ID)
}