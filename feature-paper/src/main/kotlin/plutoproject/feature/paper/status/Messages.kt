package plutoproject.feature.paper.status

import ink.pmc.advkt.component.*
import net.kyori.adventure.text.Component
import org.bukkit.entity.Entity
import plutoproject.framework.common.util.buildinfo.BuildInfo
import plutoproject.framework.common.util.chat.GENERIC_TEXT_SERVER_BRAND
import plutoproject.framework.common.util.chat.palettes.*
import plutoproject.framework.common.util.roundTo2
import plutoproject.framework.common.util.time.formatDate
import plutoproject.framework.common.util.time.formatTime
import plutoproject.framework.paper.api.statistic.LoadLevel
import plutoproject.framework.paper.api.statistic.MeasuringTime
import plutoproject.framework.paper.api.statistic.StatisticProvider
import plutoproject.framework.paper.util.server
import java.time.ZoneId
import java.time.ZonedDateTime

val INDICATOR = component {
    text("» ") with mochaSubtext0
}

fun getStatusMessage() = component {
    val entities = server.worlds.fold(mutableListOf<Entity>()) { entities, world ->
        entities.apply { addAll(world.entities) }
    }
    val entityCount = entities.size
    val tickingEntityCount = entities.filter { it.isTicking }.size

    text("\uD83D\uDD0D 服务器状态") with mochaFlamingo
    text(" (当前子服)") with mochaSubtext0
    newline()
    text("- ") with mochaSubtext0
    text("TPS：") with mochaText
    raw(getTicksPerSecond())
    newline()
    text("- ") with mochaSubtext0
    text("MSPT：") with mochaText
    raw(getMillsPerTick())
    newline()
    text("- ") with mochaSubtext0
    text("实体数：") with mochaText
    text("$entityCount ") with mochaLavender
    text("(") with mochaText
    text("$tickingEntityCount ") with mochaLavender
    text("个活跃中)") with mochaText
    newline()
    text("- ") with mochaSubtext0
    text("在线人数：") with mochaText
    text(server.onlinePlayers.size) with mochaLavender
}

private fun getTicksPerSecond(): Component {
    return StatisticProvider.getTicksPerSecond(MeasuringTime.SECONDS_10)!!.let {
        component {
            text(it.roundTo2()) with when {
                it in 16.0..18.0 -> mochaYellow
                it < 16.0 -> mochaMaroon
                else -> mochaGreen
            }
        }
    }
}

private fun getMillsPerTick(): Component {
    return component {
        text(
            StatisticProvider.getMillsPerTick(MeasuringTime.SECONDS_10)!!.roundTo2()
        ) with when (StatisticProvider.getLoadLevel()!!) {
            LoadLevel.LOW -> mochaGreen
            LoadLevel.MODERATE -> mochaYellow
            LoadLevel.HIGH -> mochaMaroon
        }
    }
}

fun getPromptMessage() = component {
    text("ℹ 说明") with mochaBlue
    newline()

    when (StatisticProvider.getLoadLevel()!!) {
        LoadLevel.LOW -> {
            text("- ") with mochaSubtext0
            text("服务器目前负载较低") with mochaGreen
            newline()
            text("- ") with mochaSubtext0
            text("可适量开启机器、进行跑图") with mochaGreen
        }

        LoadLevel.MODERATE -> {
            text("- ") with mochaSubtext0
            text("服务器目前负载中等") with mochaYellow
            newline()
            text("- ") with mochaSubtext0
            text("建议关闭不在使用的机器、酌情降低跑图速度") with mochaYellow
        }

        LoadLevel.HIGH -> {
            text("- ") with mochaSubtext0
            text("服务器目前已过载") with mochaMaroon
            newline()
            text("- ") with mochaSubtext0
            text("请关闭正在运行的机器、暂缓跑图") with mochaMaroon
        }
    }
}

val BUTTON_SERVER_STATUS_OPERATION_SHOW_VERSION = component {
    text("左键 ") with mochaLavender
    text("显示版本信息") with mochaText
}

val BUTTON_SERVER_STATUS_OPERATION_HIDE_VERSION = component {
    text("左键 ") with mochaLavender
    text("隐藏版本信息") with mochaText
}

fun getVersionMessage(timezone: ZoneId) = component {
    raw(GENERIC_TEXT_SERVER_BRAND)
    text(" ${BuildInfo.releaseName}") with mochaText
    newline()
    text("提交 ${BuildInfo.gitCommit}，分支 ${BuildInfo.gitBranch}") with mochaText
    newline()
    val date = ZonedDateTime.ofInstant(BuildInfo.buildTime, timezone)
    text("于 ${date.formatDate()} ${date.formatTime()} 构建") with mochaText
    if (!BuildInfo.isStable) {
        newline()
        newline()
        text("⚠ 警告") with mochaYellow
        newline()
        text("- ") with mochaSubtext0
        text("正在运行未完善的开发版本，不代表最终上线效果") with mochaMaroon
        newline()
        text("- ") with mochaSubtext0
        text("如遇问题请反馈给管理组，以便我们尽快解决") with mochaMaroon
    }
}
