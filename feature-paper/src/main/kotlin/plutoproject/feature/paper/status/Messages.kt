package plutoproject.feature.paper.status

import ink.pmc.advkt.component.*
import net.kyori.adventure.text.Component
import org.bukkit.entity.Entity
import plutoproject.framework.common.util.chat.palettes.*
import plutoproject.framework.common.util.roundTo2
import plutoproject.framework.paper.api.statistic.LoadLevel
import plutoproject.framework.paper.api.statistic.MeasuringTime
import plutoproject.framework.paper.api.statistic.StatisticProvider
import plutoproject.framework.paper.util.server

val INDICATOR = component {
    text("» ") with mochaSubtext0
}

fun getStatusMessage() = component {
    val entities = server.worlds.fold(mutableListOf<Entity>()) { entities, world ->
        entities.apply { addAll(world.entities) }
    }
    val entityCount = entities.size
    val tickingEntityCount = entities.filter { it.isTicking }.size

    text("\uD83D\uDD0D 服务器状态") with mochaFlamingo without italic()
    text(" (当前子服)") with mochaSubtext0 without italic()
    newline()
    text("- ") with mochaSubtext0 without italic()
    text("TPS：") with mochaText without italic()
    raw(getTicksPerSecond())
    newline()
    text("- ") with mochaSubtext0 without italic()
    text("MSPT：") with mochaText without italic()
    raw(getMillsPerTick())
    newline()
    text("- ") with mochaSubtext0 without italic()
    text("实体数：") with mochaText without italic()
    text("$entityCount ") with mochaLavender without italic()
    text("(") with mochaText without italic()
    text("$tickingEntityCount ") with mochaLavender without italic()
    text("个活跃中)") with mochaText without italic()
    newline()
    text("- ") with mochaSubtext0 without italic()
    text("在线人数：") with mochaText without italic()
    text(server.onlinePlayers.size) with mochaLavender without italic()
}

private fun getTicksPerSecond(): Component {
    return StatisticProvider.getTicksPerSecond(MeasuringTime.SECONDS_10)!!.let {
        component {
            text(it.roundTo2()) with when {
                it in 16.0..18.0 -> mochaYellow
                it < 16.0 -> mochaMaroon
                else -> mochaGreen
            } without italic()
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
        } without italic()
    }
}

fun getPromptMessage() = component {
    text("ℹ 说明") with mochaBlue without italic()
    newline()

    when (StatisticProvider.getLoadLevel()!!) {
        LoadLevel.LOW -> {
            text("- ") with mochaSubtext0 without italic()
            text("服务器目前负载较低") with mochaGreen without italic()
            newline()
            text("- ") with mochaSubtext0 without italic()
            text("可适量开启机器、进行跑图") with mochaGreen without italic()
        }

        LoadLevel.MODERATE -> {
            text("- ") with mochaSubtext0 without italic()
            text("服务器目前负载中等") with mochaYellow without italic()
            newline()
            text("- ") with mochaSubtext0 without italic()
            text("建议关闭不在使用的机器、酌情降低跑图速度") with mochaYellow without italic()
        }

        LoadLevel.HIGH -> {
            text("- ") with mochaSubtext0 without italic()
            text("服务器目前已过载") with mochaMaroon without italic()
            newline()
            text("- ") with mochaSubtext0 without italic()
            text("请关闭正在运行的机器、暂缓跑图") with mochaMaroon without italic()
        }
    }
}
