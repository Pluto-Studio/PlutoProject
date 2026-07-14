package plutoproject.feature.status.paper

import ink.pmc.advkt.component.*
import net.kyori.adventure.text.Component
import org.bukkit.entity.Entity
import plutoproject.capability.serverstatistics.api.statistic.LoadLevel
import plutoproject.capability.serverstatistics.api.statistic.MeasuringTime
import plutoproject.capability.serverstatistics.api.statistic.StatisticProvider
import plutoproject.foundation.common.text.*
import plutoproject.foundation.common.time.formatDate
import plutoproject.foundation.common.time.formatTime
import plutoproject.kernel.api.currentModuleContext
import plutoproject.kernel.api.getService
import plutoproject.kernel.api.paper.PaperModuleContext
import java.net.JarURLConnection
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.jar.JarFile
import kotlin.math.roundToInt

private val server
    get() = (currentModuleContext() as PaperModuleContext).plugin.server

private val statistics
    get() = currentModuleContext().services.getService<StatisticProvider>()

private fun Double.roundTo2(): Double = (this * 100).roundToInt() / 100.0

private fun readBuildAttribute(attribute: String): String {
    val resources = StatusFeature::class.java.classLoader.getResources("META-INF/MANIFEST.MF")
    resources.asSequence().forEach { url ->
        val connection = url.openConnection() as? JarURLConnection ?: return@forEach
        JarFile(connection.jarFileURL.toURI().path).use { jar ->
            if (jar.getEntry("plutoproject_jar_identity") == null) return@forEach
            return requireNotNull(jar.manifest.mainAttributes.getValue(attribute)) {
                "Attribute '$attribute' not found in ${connection.jarFileURL}"
            }
        }
    }
    error("Cannot find PlutoProject distribution manifest")
}

private object BuildInfo {
    val releaseName = readBuildAttribute("PlutoProject-Release-Name")
    val isStable = readBuildAttribute("PlutoProject-Release-Channel").equals("stable", ignoreCase = true)
    val gitCommit = readBuildAttribute("PlutoProject-Git-Commit")
    val gitBranch = readBuildAttribute("PlutoProject-Git-Branch")
    val buildTime = Instant.ofEpochMilli(readBuildAttribute("PlutoProject-Build-Time").toLong())
}

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

val COMMAND_STATUS_CHUNK = component {
    text("世界 ") with mochaText
    text("<world> ") with mochaLavender
    text("的区块 ") with mochaText
    text("<chunkX>, <chunkZ> ") with mochaLavender
    text("的加载等级为 ") with mochaText
    text("<level>") with mochaLavender
    text("，加载状态为 ") with mochaText
    text("<status>") with mochaLavender
}

private fun getTicksPerSecond(): Component {
    return statistics.getTicksPerSecond(MeasuringTime.SECONDS_10)!!.let {
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
            statistics.getMillsPerTick(MeasuringTime.SECONDS_10)!!.roundTo2()
        ) with when (statistics.getLoadLevel()!!) {
            LoadLevel.LOW -> mochaGreen
            LoadLevel.MODERATE -> mochaYellow
            LoadLevel.HIGH -> mochaMaroon
        }
    }
}

fun getPromptMessage() = component {
    text("ℹ 说明") with mochaBlue
    newline()

    when (statistics.getLoadLevel()!!) {
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
