package ink.pmc.daily.button

import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import ink.pmc.daily.api.Daily
import ink.pmc.daily.button.DailyState.*
import ink.pmc.daily.screens.DailyCalenderScreen
import ink.pmc.framework.chat.*
import ink.pmc.framework.interactive.LocalPlayer
import ink.pmc.framework.interactive.Item
import ink.pmc.framework.interactive.Modifier
import ink.pmc.framework.interactive.click.clickable
import ink.pmc.menu.api.dsl.buttonDescriptor
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType

val DAILY_BUTTON_DESCRIPTOR = buttonDescriptor {
    id = "daily:daily"
}

private val dailyOperation = component {
    text("左键 ") with mochaLavender without italic()
    text("打开礼记日历") with mochaText without italic()
}

private val dailyIntroduction = component {
    text("时光与点滴足迹") with mochaSubtext0 without italic()
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
            text("礼记") with mochaPink without italic()
        },
        lore = buildList {
            when (state) {
                LOADING -> add(component {
                    text("正在加载...") with mochaSubtext0 without italic()
                })

                NOT_CHECKED_IN -> add(component {
                    text("× 今日尚未到访") with mochaYellow without italic()
                })

                CHECKED_IN -> add(component {
                    text("√ 今日已到访") with mochaGreen without italic()
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