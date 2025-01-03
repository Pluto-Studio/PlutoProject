package ink.pmc.menu.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import cafe.adriel.voyager.core.model.rememberScreenModel
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.keybind
import ink.pmc.advkt.component.text
import ink.pmc.advkt.send
import ink.pmc.framework.interactive.click.clickable
import ink.pmc.framework.interactive.jetpack.Arrangement
import ink.pmc.framework.interactive.layout.Column
import ink.pmc.framework.interactive.layout.Menu
import ink.pmc.framework.interactive.layout.Row
import ink.pmc.framework.chat.UI_PAGING_SOUND
import ink.pmc.framework.chat.mochaLavender
import ink.pmc.framework.chat.mochaText
import ink.pmc.framework.interactive.*
import ink.pmc.menu.Button
import ink.pmc.menu.MenuConfig
import ink.pmc.menu.Page
import ink.pmc.menu.api.LocalMenuScreenModel
import ink.pmc.menu.api.MenuManager
import ink.pmc.menu.api.descriptor.PageDescriptor
import ink.pmc.menu.repository.UserRepository
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.event.inventory.ClickType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MenuScreen : InteractiveScreen(), KoinComponent {
    private val config by inject<MenuConfig>()
    private val userRepo by inject<UserRepository>()

    @Composable
    override fun Content() {
        val player = LocalPlayer.current
        val screenModel = rememberScreenModel { MenuScreenModelImpl() }

        LaunchedEffect(Unit) {
            val userModel = userRepo.findOrCreate(player.uniqueId)
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
            userRepo.saveOrUpdate(
                userModel.copy(
                    wasOpenedBefore = true
                )
            )
        }

        CompositionLocalProvider(
            LocalMenuScreenModel provides screenModel,
        ) {
            Menu(
                title = Component.text("手账"),
                rows = 6,
                topBorderAttachment = {
                    ItemSpacer()
                    Row(modifier = Modifier.height(1).width(7), horizontalArrangement = Arrangement.Center) {
                        val pages = MenuManager.pages.take(7)
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
                    MenuManager.getPageDescriptor(currentPageId)
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
            val button = MenuManager.getButtonDescriptor(customButtonId)
                ?: error("Custom page button with id $customButtonId not registered")
            val buttonComponent = MenuManager.getButton(button)
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
                    text("左键 ") with mochaLavender without italic()
                    text("切换至此页面") with mochaText without italic()
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
                val button = page.buttons.firstOrNull { it.pattern == pattern }
                    ?: error("Button with pattern $pattern not found in config")
                Button(button)
            }
        }
    }

    @Suppress("FunctionName")
    @Composable
    private fun Button(button: Button) {
        val descriptor = remember(button) {
            MenuManager.getButtonDescriptor(button.id)
                ?: error("ButtonDescriptor with id ${button.id} not registered")
        }
        val buttonComponent = remember(button) {
            MenuManager.getButton(descriptor) ?: error("Unexpected")
        }
        buttonComponent()
    }
}