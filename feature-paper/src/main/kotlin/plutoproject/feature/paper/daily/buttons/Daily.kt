package plutoproject.feature.paper.daily.buttons

import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import plutoproject.feature.paper.api.daily.Daily
import plutoproject.feature.paper.api.menu.dsl.ButtonDescriptor
import plutoproject.feature.paper.daily.buttons.DailyState.*
import plutoproject.feature.paper.daily.screens.DailyCalenderScreen
import plutoproject.framework.common.util.chat.palettes.*
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.api.interactive.click.clickable
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.api.interactive.modifiers.Modifier

val DailyButtonDescriptor = ButtonDescriptor {
    id = "daily:daily"
}

private val dailyOperation = component {
    text("左键 ") with mochaLavender
    text("打开礼记日历") with mochaText
}

private val dailyIntroduction = component {
    text("时光与点滴足迹") with mochaSubtext0
}

private enum class DailyState {
    LOADING, NOT_CHECKED_IN, CHECKED_IN
}

@Composable
@Suppress("FunctionName")
fun Daily() {
    val player = LocalPlayer.current
    val navigator = LocalNavigator.currentOrThrow
    var state by remember { mutableStateOf(LOADING) }
    LaunchedEffect(Unit) {
        state = if (Daily.isCheckedInToday(player.uniqueId)) CHECKED_IN else NOT_CHECKED_IN
    }
    Item(
        material = Material.NAME_TAG,
        name = component {
            text("礼记") with mochaPink
        },
        lore = buildList {
            when (state) {
                LOADING -> add(component {
                    text("正在加载...") with mochaSubtext0
                })

                NOT_CHECKED_IN -> add(component {
                    text("× 今日尚未到访") with mochaYellow
                })

                CHECKED_IN -> add(component {
                    text("√ 今日已到访") with mochaGreen
                })
            }
            add(dailyIntroduction)
            add(Component.empty())
            add(dailyOperation)
        },
        modifier = Modifier.clickable {
            if (clickType != ClickType.LEFT) return@clickable
            navigator.push(DailyCalenderScreen())
        }
    )
}
