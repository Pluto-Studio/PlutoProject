package ink.pmc.daily.screens

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.rememberScreenModel
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import ink.pmc.daily.*
import ink.pmc.daily.api.Daily
import ink.pmc.daily.api.DailyHistory
import ink.pmc.framework.interactive.click.clickable
import ink.pmc.framework.interactive.jetpack.Arrangement
import ink.pmc.framework.interactive.layout.Column
import ink.pmc.framework.interactive.layout.Menu
import ink.pmc.framework.interactive.layout.Row
import ink.pmc.framework.chat.UI_PAGING_SOUND
import ink.pmc.framework.chat.UI_SUCCEED_SOUND
import ink.pmc.framework.chat.replace
import ink.pmc.framework.dsl.itemStack
import ink.pmc.framework.time.formatDate
import ink.pmc.framework.time.formatTime
import ink.pmc.framework.time.zoneId
import ink.pmc.framework.trimmed
import ink.pmc.framework.chat.mochaFlamingo
import ink.pmc.framework.chat.mochaSubtext0
import ink.pmc.framework.chat.mochaText
import ink.pmc.framework.interactive.*
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.meta.SkullMeta
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

class DailyCalenderScreen : InteractiveScreen() {
    private val localModel: ProvidableCompositionLocal<DailyCalenderScreenModel> =
        staticCompositionLocalOf { error("Unexpected") }

    @Composable
    override fun Content() {
        val player = LocalPlayer.current
        val currentDate by remember { mutableStateOf(ZonedDateTime.now(player.zoneId)) }
        val model = rememberScreenModel { DailyCalenderScreenModel(player) }
        LaunchedEffect(Unit) {
            model.init()
        }
        CompositionLocalProvider(localModel provides model) {
            Menu(
                title = UI_TITLE.replace("<time>", currentDate.formatDate()),
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
                                    text("正在加载...") with mochaSubtext0 without italic()
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
        val now by remember { mutableStateOf(LocalDate.now(player.zoneId)) }

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
                    it.itemName(DAY.replace("<time>", date.formatDate()))
                    it.lore(
                        when {
                            state == 0 && date == now -> DAY_LORE.replace(
                                "<reward>",
                                Component.text("${model.user?.getReward()?.trimmed() ?: -1}")
                            ).toList()

                            state == 0 && date.isBefore(now) -> DAY_LORE_PAST
                            state == 1 -> history?.let { history ->
                                val lore =
                                    if (history.rewarded > 0) DAY_LORE_CHECKED_IN_REWARDED else DAY_LORE_CHECKED_IN
                                lore.replace(
                                    "<time>",
                                    Component.text(
                                        LocalDateTime.ofInstant(history.createdAt, player.zoneId).formatTime()
                                    )
                                ).replace("<reward>", history.rewarded.trimmed())
                            }?.toList() ?: emptyList()

                            date.isAfter(now) -> DAY_LORE_FUTURE
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
            model.yearMonth == model.realTime -> NAVIGATE_LORE
            model.canGoPrevious() -> NAVIGATE_LORE_CAN_RESET
            else -> NAVIGATE_LORE_PREV_REACHED
        }
        Item(
            material = Material.ARROW,
            name = NAVIGATE
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
            itemStack = itemStack(Material.PLAYER_HEAD) {
                displayName {
                    text(player.name) with mochaFlamingo without italic()
                }
                lore {
                    text("本月已到访 ") with mochaSubtext0 without italic()
                    text("${model.checkInDays} ") with mochaText without italic()
                    text("天，连续 ") with mochaSubtext0 without italic()
                    text("${model.accumulatedDays} ") with mochaText without italic()
                    text("天") with mochaSubtext0 without italic()
                }
                meta {
                    this as SkullMeta
                    playerProfile = player.playerProfile
                }
            }
        )
    }
}