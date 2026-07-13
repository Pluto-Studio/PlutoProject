package plutoproject.capability.interactive.api.layout.list

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import net.kyori.adventure.text.Component
import org.bukkit.Material
import plutoproject.foundation.common.text.mochaSubtext0
import plutoproject.capability.interactive.api.*
import plutoproject.capability.interactive.api.animations.loadingIconAnimation
import plutoproject.capability.interactive.api.animations.spinnerAnimation
import plutoproject.capability.interactive.api.components.Item
import plutoproject.capability.interactive.api.components.SeparatePageTuner
import plutoproject.capability.interactive.api.components.SeparatePageTunerMode
import plutoproject.capability.interactive.api.components.Spacer
import plutoproject.capability.interactive.api.jetpack.Arrangement
import plutoproject.capability.interactive.api.layout.Column
import plutoproject.capability.interactive.api.canvas.Menu
import plutoproject.capability.interactive.api.layout.Row
import plutoproject.capability.interactive.api.layout.VerticalGrid
import plutoproject.capability.interactive.api.modifiers.Modifier
import plutoproject.capability.interactive.api.modifiers.fillMaxSize
import plutoproject.capability.interactive.api.modifiers.fillMaxWidth
import plutoproject.capability.interactive.api.modifiers.height
import plutoproject.capability.interactive.api.modifiers.width

abstract class ListMenu<E, M : ListMenuModel<E>> : InteractiveScreen() {
    val LocalListMenuModel: ProvidableCompositionLocal<M> =
        staticCompositionLocalOf { error("Uninitialized") }
    val LocalListMenuOptions: ProvidableCompositionLocal<ListMenuOptions> =
        staticCompositionLocalOf { error("Uninitialized") }

    @Composable
    abstract fun modelProvider(): M

    @Composable
    open fun reloadConditionProvider(): Array<Any> {
        LocalNavigator
        val model = LocalListMenuModel.current
        return arrayOf(model.page)
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Content() {
        val modelInstance = modelProvider() as ScreenModel
        val model = rememberScreenModel { modelInstance }
        val options = remember { ListMenuOptions() }
        CompositionLocalProvider(
            LocalListMenuModel provides model as M,
            LocalListMenuOptions provides options
        ) {
            MenuLayout()
        }
    }

    @Composable
    @Suppress("FunctionName")
    abstract fun Element(obj: E)

    @Composable
    @Suppress("FunctionName")
    open fun MenuLayout() {
        val options = LocalListMenuOptions.current
        require(options.rows >= 3) { "Menu must have at least 3 rows" }
        Menu(
            title = options.title,
            rows = options.rows,
            topBorder = options.topBorder,
            bottomBorder = options.bottomBorder,
            leftBorder = options.leftBorder,
            rightBorder = options.rightBorder,
            bottomBorderAttachment = { BottomBorderAttachment() },
            background = options.background,
            centerBackground = options.centerBackground
        ) {
            MenuContent()
        }
    }

    @Composable
    @Suppress("FunctionName")
    open fun MenuContent() {
        val model = LocalListMenuModel.current
        LaunchedEffect(*reloadConditionProvider()) {
            model.loadPageContents()
        }
        if (model.isLoading) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Row(modifier = Modifier.fillMaxWidth().height(2), horizontalArrangement = Arrangement.Center) {
                    Item(
                        material = loadingIconAnimation(),
                        name = Component.text("${spinnerAnimation()} 正在加载...").color(mochaSubtext0)
                    )
                }
            }
            return
        }
        if (model.contents.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Row(modifier = Modifier.fillMaxWidth().height(2), horizontalArrangement = Arrangement.Center) {
                    Item(
                        material = Material.MINECART,
                        name = component {
                            text("这里没有内容 :(") with mochaSubtext0
                        }
                    )
                }
            }
            return
        }
        VerticalGrid(modifier = Modifier.fillMaxSize()) {
            model.contents.forEach {
                Element(it)
            }
        }
    }

    @Composable
    @Suppress("FunctionName")
    open fun BottomBorderAttachment() {
        if (LocalListMenuModel.current.isLoading) return
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center) {
            PreviousTurner()
            Spacer(modifier = Modifier.height(1).width(1))
            NextTurner()
        }
    }

    @Composable
    @Suppress("FunctionName")
    open fun PreviousTurner() {
        val model = LocalListMenuModel.current
        val options = LocalListMenuOptions.current
        if (model.pageCount <= 1) {
            Spacer(modifier = Modifier.height(1).width(1))
            return
        }
        SeparatePageTuner(
            icon = options.previousTurnerIcon,
            mode = SeparatePageTunerMode.PREVIOUS,
            current = model.page + 1,
            total = model.pageCount,
            turn = model::previousPage
        )
    }

    @Composable
    @Suppress("FunctionName")
    open fun NextTurner() {
        val model = LocalListMenuModel.current
        val options = LocalListMenuOptions.current
        if (model.pageCount <= 1) {
            Spacer(modifier = Modifier.height(1).width(1))
            return
        }
        SeparatePageTuner(
            icon = options.nextTurnerIcon,
            mode = SeparatePageTunerMode.NEXT,
            current = model.page + 1,
            total = model.pageCount,
            turn = model::nextPage
        )
    }
}
