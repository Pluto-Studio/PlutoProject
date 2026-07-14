package plutoproject.feature.menu.paper.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import cafe.adriel.voyager.core.model.rememberScreenModel
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.keybind
import ink.pmc.advkt.component.text
import ink.pmc.advkt.send
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.event.inventory.ClickType
import plutoproject.kernel.api.koinInject
import plutoproject.feature.menu.api.paper.LocalMenuScreenModel
import plutoproject.feature.menu.api.paper.MenuManager
import plutoproject.feature.menu.api.paper.descriptor.PageDescriptor
import plutoproject.feature.menu.paper.Button
import plutoproject.feature.menu.paper.MENU_USER_MODEL_PERSIST_KEY
import plutoproject.feature.menu.paper.MenuConfig
import plutoproject.feature.menu.paper.Page
import plutoproject.feature.menu.paper.models.PersistUserModel
import plutoproject.feature.menu.paper.models.UserModelTypeAdapter
import plutoproject.capability.databasepersist.api.DatabasePersist
import plutoproject.foundation.common.text.UI_PAGING_SOUND
import plutoproject.foundation.common.text.mochaLavender
import plutoproject.foundation.common.text.mochaText
import plutoproject.capability.interactive.api.InteractiveScreen
import plutoproject.capability.interactive.api.LocalPlayer
import plutoproject.capability.interactive.api.canvas.Menu
import plutoproject.capability.interactive.api.click.clickable
import plutoproject.capability.interactive.api.components.Item
import plutoproject.capability.interactive.api.components.ItemSpacer
import plutoproject.capability.interactive.api.jetpack.Arrangement
import plutoproject.capability.interactive.api.layout.Column
import plutoproject.capability.interactive.api.layout.Row
import plutoproject.capability.interactive.api.modifiers.*

class MenuScreen : InteractiveScreen() {
    private val config by koinInject<MenuConfig>()

    @Composable
    override fun Content() {
        val player = LocalPlayer.current
        val screenModel = rememberScreenModel { MenuScreenModelImpl() }

        LaunchedEffect(Unit) {
            val container = plutoproject.kernel.api.koinGet<DatabasePersist>().getContainer(player.uniqueId)
            val userModel = container.getOrDefault(MENU_USER_MODEL_PERSIST_KEY, UserModelTypeAdapter, PersistUserModel())
            if (userModel.wasOpenedBefore) return@LaunchedEffect
            player.send {
                text("小提示: 你可以使用 ") with mochaText
                keybind("key.sneak") with mochaLavender
                text(" + ") with mochaLavender
                keybind("key.swapOffhand") with mochaLavender
                text(" 或 ") with mochaText
                text("/menu ") with mochaLavender
                text("来打开「手账」") with mochaText
            }
            container.set(MENU_USER_MODEL_PERSIST_KEY, UserModelTypeAdapter, userModel.copy(wasOpenedBefore = true))
            container.save()
        }

        CompositionLocalProvider(LocalMenuScreenModel provides screenModel) {
            Menu(
                title = Component.text("手账"),
                rows = 6,
                topBorderAttachment = {
                    ItemSpacer()
                    Row(modifier = Modifier.height(1).width(7), horizontalArrangement = Arrangement.Center) {
                        val pages = plutoproject.kernel.api.koinGet<MenuManager>().pages.take(7)
                        val canAddCap = pages.size in 2..4
                        pages.forEachIndexed { i, e ->
                            Paging(e)
                            if (canAddCap && i != pages.lastIndex) {
                                ItemSpacer()
                            }
                        }
                    }
                    ItemSpacer()
                }
            ) {
                val currentPageId = screenModel.currentPageId
                val currentPage = remember(currentPageId) {
                    plutoproject.kernel.api.koinGet<MenuManager>().getPageDescriptor(currentPageId)
                        ?: error("PageDescriptor with id $currentPageId not registered")
                }
                Page(currentPage)
            }
        }
    }

    @Suppress("FunctionName")
    @Composable
    private fun Paging(descriptor: PageDescriptor) {
        val screenModel = LocalMenuScreenModel.current
        val customButtonId = descriptor.customPagingButtonId
        if (customButtonId != null) {
            val button = plutoproject.kernel.api.koinGet<MenuManager>().getButtonDescriptor(customButtonId)
                ?: error("Custom page button with id $customButtonId not registered")
            val buttonComponent = plutoproject.kernel.api.koinGet<MenuManager>().getButton(button)
                ?: error("Unexpected")
            buttonComponent()
            return
        }
        val player = LocalPlayer.current
        Item(
            material = descriptor.icon,
            name = descriptor.name.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE),
            lore = buildList {
                addAll(descriptor.description.map { desc ->
                    desc.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                })
                if (screenModel.currentPageId == descriptor.id) return@buildList
                add(Component.empty())
                add(component {
                    text("左键 ") with mochaLavender
                    text("切换至此页面") with mochaText
                })
            },
            modifier = Modifier.clickable {
                if (clickType != ClickType.LEFT) return@clickable
                if (screenModel.currentPageId == descriptor.id) return@clickable
                screenModel.currentPageId = descriptor.id
                player.playSound(UI_PAGING_SOUND)
            }
        )
    }

    @Suppress("FunctionName")
    @Composable
    private fun Page(descriptor: PageDescriptor) {
        Column(modifier = Modifier.fillMaxSize()) {
            val page = remember(descriptor) {
                config.pages.firstOrNull { it.id == descriptor.id }
                    ?: error("Page with id ${descriptor.id} not found in config")
            }
            page.patterns.forEach {
                Line(it, page)
            }
        }
    }

    @Suppress("FunctionName")
    @Composable
    private fun Line(patterns: String, page: Page) {
        Row(modifier = Modifier.fillMaxWidth().height(1)) {
            patterns.forEach { pattern ->
                if (pattern.isWhitespace()) {
                    ItemSpacer()
                    return@forEach
                }
                val button = page.buttons.firstOrNull { it.pattern.single() == pattern }
                    ?: error("Button with pattern $pattern not found in config")
                Button(button)
            }
        }
    }

    @Suppress("FunctionName")
    @Composable
    private fun Button(button: Button) {
        val descriptor = remember(button) {
            plutoproject.kernel.api.koinGet<MenuManager>().getButtonDescriptor(button.id)
                ?: error("ButtonDescriptor with id ${button.id} not registered")
        }
        val buttonComponent = remember(button) {
            plutoproject.kernel.api.koinGet<MenuManager>().getButton(descriptor) ?: error("Unexpected")
        }
        buttonComponent()
    }
}
