package plutoproject.feature.menu.paper.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import plutoproject.feature.menu.api.paper.MenuPrebuilt
import plutoproject.feature.menu.api.paper.MenuScreenModel

class MenuScreenModelImpl : ScreenModel, MenuScreenModel {
    override var currentPageId by mutableStateOf(MenuPrebuilt.Pages.HOME_PAGE_ID)
}
