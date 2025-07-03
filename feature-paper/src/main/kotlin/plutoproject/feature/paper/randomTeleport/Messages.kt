package plutoproject.feature.paper.randomTeleport

import ink.pmc.advkt.component.*
import ink.pmc.advkt.title.*
import net.kyori.adventure.util.Ticks
import plutoproject.feature.paper.api.randomTeleport.RandomTeleportManager
import plutoproject.framework.common.util.chat.palettes.*
import plutoproject.framework.common.util.chat.toFormattedComponent
import java.time.Duration
import kotlin.time.toKotlinDuration

val RANDOM_TELEPORTED = component {
    text("已将你传送到 ") with mochaPink
    text("<location>") with mochaText
    newline()
    text("本次传送尝试 ") with mochaSubtext0
    text("<attempts> ") with mochaText
    text("次，耗时 ") with mochaSubtext0
    text("<lastLookupTime>") with mochaText
}

val RANDOM_TELEPORTED_WITH_COST = component {
    raw(RANDOM_TELEPORTED)
    text("，花费 ") with mochaSubtext0
    text("<amount><symbol>") with mochaText
}

val RANDOM_TELEPORT_FAILED_BALANCE_NOT_ENOUGH = component {
    text("货币不足，进行随机传送需要 ") with mochaMaroon
    text("<amount> <symbol>") with mochaText
    newline()
    text("你当前拥有 ") with mochaSubtext0
    text("<balance> <symbol>") with mochaText
}

val RANDOM_TELEPORT_SEARCH_TITLE = title {
    times {
        fadeIn(Ticks.duration(5))
        stay(Duration.ofMinutes(1))
        fadeOut(Duration.ZERO)
    }
    mainTitle {
        text("传送中") with mochaPink
    }
    subTitle {
        text("正在搜寻目的地") with mochaText
    }
}

val RANDOM_TELEPORT_SEARCH_FAILED = component {
    text("这次运气似乎不太好...") with mochaYellow
    newline()
    text("如果此问题总是发生，请报告给管理组") with mochaSubtext0
}

val RANDOM_TELEPORT_FAILED_IN_PROGRESS = component {
    text("你已有一个正在处理的随机传送操作，请不要着急") with mochaYellow
}

val COMMAND_RTP_FAILED_WORLD_NOT_ENABLED = component {
    text("该世界未启用随机传送") with mochaMaroon
}

val COMMAND_RTP_COOLDOWN = component {
    text("传送冷却中...") with mochaSubtext0
    newline()
    text("请 ") with mochaSubtext0
    text("<time> ") with mochaText
    text("后再试") with mochaSubtext0
}

val COMMAND_RTP_STATUS
    get() = component {
        text("随机传送状态：") with mochaFlamingo
        newline()

        text("  - ") with mochaSubtext0
        text("已处理刻数：") with mochaText
        text("${RandomTeleportManager.tickCount}") with mochaLavender
        newline()

        text("  - ") with mochaSubtext0
        text("上次刻处理用时：") with mochaText
        raw(
            Duration.ofMillis(RandomTeleportManager.lastTickTime)
                .toKotlinDuration()
                .toFormattedComponent()
        ) with mochaLavender

        newline()
        empty()
        newline()

        text("世界缓存数：") with mochaFlamingo
        newline()

        val worlds = RandomTeleportManager.enabledWorlds
        worlds.forEach {
            val caches = RandomTeleportManager.getCaches(it).size
            text("  - ") with mochaSubtext0
            text("${it.name}：") with mochaText
            text(caches) with mochaLavender
            if (worlds.last() != it) {
                newline()
            }
        }
    }
