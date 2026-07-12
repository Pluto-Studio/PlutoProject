package plutoproject.kernel.common

import org.koin.core.logger.Level
import org.koin.core.logger.Logger

internal class ModuleKoinLogger(
    private val logger: System.Logger,
) : Logger(Level.INFO) {
    override fun display(level: Level, msg: String) {
        val systemLevel = when (level) {
            Level.DEBUG -> System.Logger.Level.DEBUG
            Level.INFO -> System.Logger.Level.INFO
            Level.WARNING -> System.Logger.Level.WARNING
            Level.ERROR -> System.Logger.Level.ERROR
            Level.NONE -> return
        }
        logger.log(systemLevel, msg)
    }
}
