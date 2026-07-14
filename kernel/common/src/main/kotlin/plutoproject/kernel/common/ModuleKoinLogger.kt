package plutoproject.kernel.common

import org.koin.core.logger.Level
import org.koin.core.logger.Logger
import java.util.logging.Level as JulLevel
import java.util.logging.Logger as JulLogger

internal class ModuleKoinLogger(
    private val logger: JulLogger,
) : Logger(Level.INFO) {
    override fun display(level: Level, msg: String) {
        val julLevel = when (level) {
            Level.DEBUG -> JulLevel.FINE
            Level.INFO -> JulLevel.INFO
            Level.WARNING -> JulLevel.WARNING
            Level.ERROR -> JulLevel.SEVERE
            Level.NONE -> return
        }
        logger.log(julLevel, msg)
    }
}
