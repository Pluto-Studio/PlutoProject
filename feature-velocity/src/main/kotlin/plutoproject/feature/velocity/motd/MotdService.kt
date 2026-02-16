package plutoproject.feature.velocity.motd

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import plutoproject.framework.common.util.config.loadConfig
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.logging.Logger

class MotdService(
    private val configFile: File,
    private val logger: Logger,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    private data class Template(
        val startDate: LocalDate,
        val line1: String,
        val line2: String,
    )

    @Volatile
    private var template: Template = loadTemplate()

    fun reload(): Boolean {
        return runCatching {
            template = loadTemplate()
        }.onFailure {
            logger.severe("MOTD 配置加载失败: ${it.message}")
        }.isSuccess
    }

    fun renderMotd(): Component {
        val t = template
        val days = ChronoUnit.DAYS.between(t.startDate, LocalDate.now(zoneId))
            .coerceAtLeast(0)
            .toString()

        val raw = buildString {
            append(t.line1.replace("\$days", days))
            append('\n')
            append(t.line2.replace("\$days", days))
        }

        return MiniMessage.miniMessage().deserialize(raw)
    }

    private fun loadTemplate(): Template {
        val config: MotdConfig = loadConfig(configFile)
        val startDate = try {
            LocalDate.parse(config.startDate)
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("start-date 格式不正确，期望 yyyy-MM-dd，实际为: ${config.startDate}", e)
        }
        return Template(
            startDate = startDate,
            line1 = config.line1,
            line2 = config.line2,
        )
    }
}
