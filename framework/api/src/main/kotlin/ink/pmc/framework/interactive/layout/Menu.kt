package ink.pmc.framework.interactive.layout

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.LocalNavigator
import ink.pmc.advkt.component.newline
import ink.pmc.advkt.component.text
import ink.pmc.advkt.send
import ink.pmc.framework.frameworkLogger
import ink.pmc.framework.interactive.canvas.Chest
import ink.pmc.framework.interactive.jetpack.Arrangement
import ink.pmc.framework.chat.mochaMaroon
import ink.pmc.framework.chat.mochaSubtext0
import ink.pmc.framework.interactive.*
import net.kyori.adventure.text.Component
import java.util.logging.Level

@Suppress("FunctionName")
@Composable
fun Menu(
    title: Component = Component.empty(),
    rows: Int = 5,
    topBorder: Boolean = true,
    bottomBorder: Boolean = true,
    leftBorder: Boolean = true,
    rightBorder: Boolean = true,
    topBorderAttachment: ComposableFunction = { Back() },
    bottomBorderAttachment: ComposableFunction = {},
    leftBorderAttachment: ComposableFunction = {},
    rightBorderAttachment: ComposableFunction = {},
    navigatorWarn: Boolean = true,
    background: Boolean = true,
    centerBackground: Boolean = false,
    contents: ComposableFunction
) {
    require(rows in 2..6) { "Row count must be in range: [2, 6]" }
    if (LocalNavigator.current == null && navigatorWarn) {
        frameworkLogger.log(
            Level.WARNING,
            "A menu layout was opened without Navigator context",
            IllegalStateException()
        )
        LocalPlayer.current.send {
            text("你打开了一个没有 Navigator 上下文的菜单布局") with mochaMaroon
            newline()
            text("这可能会导致一些问题，请将其报告给管理组") with mochaSubtext0
        }
    }
    Chest(title = title, modifier = Modifier.size(width = 9, height = rows)) {
        // Background
        if (background) {
            Placeholder(modifier = Modifier.fillMaxSize())
        }
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
            // Top
            if (topBorder) Box(modifier = Modifier.fillMaxWidth().height(1)) {
                Spacer(modifier = Modifier.fillMaxSize())
                Row(modifier = Modifier.fillMaxSize()) {
                    topBorderAttachment()
                }
            }
            val height = if (topBorder && bottomBorder) rows - 2
            else if (!topBorder && !bottomBorder) rows - 1
            else rows
            if (height >= 1) Row(
                modifier = Modifier.fillMaxWidth().height(height)
            ) {
                // Left
                if (leftBorder) Box(modifier = Modifier.fillMaxHeight().width(1)) {
                    Spacer(modifier = Modifier.fillMaxSize())
                    Column(modifier = Modifier.fillMaxSize()) {
                        leftBorderAttachment()
                    }
                }
                // Contents
                Box(
                    modifier = Modifier.fillMaxHeight().width(
                        if (leftBorder && rightBorder) 7
                        else if (!leftBorder && !rightBorder) 9
                        else 8
                    )
                ) {
                    if (!centerBackground) {
                        Empty(modifier = Modifier.fillMaxSize())
                    }
                    VerticalGrid(modifier = Modifier.fillMaxSize()) {
                        contents()
                    }
                }
                // Right
                if (rightBorder) Box(modifier = Modifier.fillMaxHeight().width(1)) {
                    Spacer(modifier = Modifier.fillMaxSize())
                    Column(modifier = Modifier.fillMaxSize()) {
                        rightBorderAttachment()
                    }
                }
            }
            // Bottom
            if (bottomBorder) Box(modifier = Modifier.fillMaxWidth().height(1)) {
                Spacer(modifier = Modifier.fillMaxSize())
                Row(modifier = Modifier.fillMaxSize()) {
                    bottomBorderAttachment()
                }
            }
        }
    }
}