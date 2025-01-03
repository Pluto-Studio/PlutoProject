package ink.pmc.menu.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import ink.pmc.menu.api.MenuPrebuilt
import ink.pmc.menu.api.MenuScreenModel

class MenuScreenModelImpl : ScreenModel, MenuScreenModel {
    override var currentPageId by mutableStateOf(MenuPrebuilt.Pages.HOME_PAGE_ID)
}