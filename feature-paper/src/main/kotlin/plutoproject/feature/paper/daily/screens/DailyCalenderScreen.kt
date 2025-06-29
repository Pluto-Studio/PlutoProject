package plutoproject.feature.paper.daily.screens

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.rememberScreenModel
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.text
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.meta.SkullMeta
import plutoproject.feature.paper.api.daily.Daily
import plutoproject.feature.paper.api.daily.DailyHistory
import plutoproject.feature.paper.daily.*
import plutoproject.framework.common.util.chat.UI_PAGING_SOUND
import plutoproject.framework.common.util.chat.UI_SUCCEED_SOUND
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.chat.component.replaceInComponent
import plutoproject.framework.common.util.chat.palettes.mochaFlamingo
import plutoproject.framework.common.util.chat.palettes.mochaSubtext0
import plutoproject.framework.common.util.chat.palettes.mochaText
import plutoproject.framework.common.util.time.formatDate
import plutoproject.framework.common.util.time.formatTime
import plutoproject.framework.common.util.trimmedString
import plutoproject.framework.paper.api.interactive.InteractiveScreen
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.api.interactive.canvas.Menu
import plutoproject.framework.paper.api.interactive.click.clickable
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.api.interactive.components.Spacer
import plutoproject.framework.paper.api.interactive.jetpack.Arrangement
import plutoproject.framework.paper.api.interactive.layout.Column
import plutoproject.framework.paper.api.interactive.layout.Row
import plutoproject.framework.paper.api.interactive.layout.VerticalGrid
import plutoproject.framework.paper.api.interactive.modifiers.*
import plutoproject.framework.paper.api.provider.timezone
import plutoproject.framework.paper.util.dsl.ItemStack
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

class DailyCalenderScreen : InteractiveScreen() {
    private val localModel: ProvidableCompositionLocal<DailyCalenderScreenModel> =
        staticCompositionLocalOf { error("Unexpected") }

    @Composable
    override fun Content() {
        val player = LocalPlayer.current
        val currentDate by remember { mutableStateOf(ZonedDateTime.now(player.timezone.toZoneId())) }
        val model = rememberScreenModel { DailyCalenderScreenModel(player) }
        LaunchedEffect(Unit) {
            model.init()
        }
        CompositionLocalProvider(localModel provides model) {
            Menu(
                title = UI_CALENDAR_TITLE.replace("<time>", currentDate.formatDate()),
                rows = 6,
                leftBorder = false,
                rightBorder = false,
                bottomBorderAttachment = {
                    if (model.isLoading) return@Menu
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center) {
                        Navigate()
                        Spacer(modifier = Modifier.size(1))
                        Player()
                    }
                }
            ) {
                val yearMonth = model.yearMonth
                val days = yearMonth.lengthOfMonth()
                if (model.isLoading) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                        Row(modifier = Modifier.fillMaxWidth().height(2), horizontalArrangement = Arrangement.Center) {
                            Item(
                                material = Material.CHEST_MINECART,
                                name = component {
                                    text("正在加载...") with mochaSubtext0
                                }
                            )
                        }
                    }
                    return@Menu
                }
                VerticalGrid(modifier = Modifier.fillMaxSize()) {
                    repeat(days) {
                        val day = it + 1
                        val date = yearMonth.atDay(day)
                        Day(date, model.getHistory(date))
                    }
                }
            }
        }
    }

    @Composable
    @Suppress("FunctionName")
    private fun Day(date: LocalDate, history: DailyHistory?) {
        val model = localModel.current
        val player = LocalPlayer.current
        val coroutineScope = rememberCoroutineScope()

        /*
        * 0 -> 未签到
        * 1 -> 已签到
        * */
        // 可能残留状态，让它在 date 变化时重新初始化
        var state by remember(date, history) { mutableStateOf(if (history != null) 1 else 0) }
        val now by remember { mutableStateOf(LocalDate.now(player.timezone.toZoneId())) }

        val head = when {
            state == 0 && date == now -> yellowExclamationHead
            state == 0 && date.isBefore(now) -> redCrossHead
            state == 1 -> greenCheckHead
            date.isAfter(now) -> grayQuestionHead
            else -> error("Unreachable")
        }

        Item(
            itemStack = head.clone().apply {
                amount = date.dayOfMonth
                editMeta {
                    it.displayName(UI_CALENDAR_DAY_DATE.replace("<date>", date.formatDate()))
                    it.lore(
                        when {
                            state == 0 && date == now -> UI_CALENDAR_DAY_LORE_TODAY_NOT_CHECKED_IN.replaceInComponent(
                                "<reward>",
                                Component.text("${model.user?.getReward()?.trimmedString() ?: -1}")
                            ).toList()

                            state == 0 && date.isBefore(now) -> UI_CALENDAR_DAY_LORE_PAST_NOT_CHECKED_IN
                            state == 1 -> history?.let { history ->
                                val lore =
                                    if (history.rewarded > 0) UI_CALENDAR_DAY_LORE_CHECKED_IN_REWARD else UI_CALENDAR_DAY_LORE_CHECKED_IN
                                val time =
                                    LocalDateTime.ofInstant(history.createdAt, player.timezone.toZoneId()).formatTime()
                                lore.replaceInComponent("<time>", Component.text(time))
                                    .replaceInComponent("<reward>", history.rewarded.trimmedString())
                            }?.toList() ?: emptyList()

                            date.isAfter(now) -> UI_CALENDAR_DAY_LORE_FEATURE
                            else -> error("Unreachable")
                        }
                    )
                    it.setEnchantmentGlintOverride(date == now)
                }
            },
            modifier = Modifier.clickable {
                when (clickType) {
                    ClickType.LEFT -> {
                        if (state == 0 && date == now) {
                            coroutineScope.launch {
                                if (Daily.isCheckedInToday(player.uniqueId)) return@launch
                                Daily.checkIn(player.uniqueId).also {
                                    model.loadedHistories.add(it)
                                    model.accumulatedDays++
                                }
                            }
                            state = 1
                            player.playSound(UI_SUCCEED_SOUND)
                        }
                    }

                    else -> {}
                }
            }
        )
    }

    @Composable
    @Suppress("FunctionName")
    private fun Navigate() {
        val model = localModel.current
        val lore = when {
            model.yearMonth == model.realTime -> UI_CALENDAR_NAVIGATION_LORE_TODAY
            model.canGoPrevious() -> UI_CALENDAR_NAVIGATION_LORE_DIFFERENT_DAY
            else -> UI_CALENDAR_NAVIGATION_LORE_PREVIOUS_LIMIT_REACHED
        }
        Item(
            material = Material.ARROW,
            name = UI_CALENDAR_NAVIGATION
                .replace("<year>", model.yearMonth.year)
                .replace("<month>", model.yearMonth.month.value),
            lore = lore,
            modifier = Modifier.clickable {
                when (clickType) {
                    ClickType.LEFT -> {
                        if (!(model.canGoPrevious())) return@clickable
                        model.goPrevious()
                        whoClicked.playSound(UI_PAGING_SOUND)
                    }

                    ClickType.RIGHT -> {
                        model.goNext()
                        whoClicked.playSound(UI_PAGING_SOUND)
                    }

                    ClickType.SHIFT_LEFT -> {
                        if (model.yearMonth == model.realTime) return@clickable
                        model.backNow()
                        whoClicked.playSound(UI_PAGING_SOUND)
                    }

                    else -> {}
                }
            }
        )
    }

    @Composable
    @Suppress("FunctionName")
    private fun Player() {
        val model = localModel.current
        val player = LocalPlayer.current
        Item(
            itemStack = ItemStack(Material.PLAYER_HEAD) {
                displayName {
                    text(player.name) with mochaFlamingo
                }
                lore {
                    text("本月已到访 ") with mochaSubtext0
                    text("${model.checkInDays} ") with mochaText
                    text("天，连续 ") with mochaSubtext0
                    text("${model.accumulatedDays} ") with mochaText
                    text("天") with mochaSubtext0
                }
                meta {
                    this as SkullMeta
                    playerProfile = player.playerProfile
                }
            }
        )
    }
}
