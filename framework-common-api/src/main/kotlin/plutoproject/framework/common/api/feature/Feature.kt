package plutoproject.framework.common.api.feature

import java.io.File
import java.nio.file.Path
import java.util.logging.Logger

interface Feature<S : Any, P : Any> {
    val id: String
    val state: State
    val platform: Platform
    val server: S
    val plugin: P
    val logger: Logger
    val dataFolder: File
    val resourcePrefixInJar: String

    fun onLoad() {}

    fun onEnable() {}

    fun onReload() {}

    fun onDisable() {}

    fun saveConfig(resourcePrefix: String? = null): File

    fun saveResource(
        path: String,
        outputPath: Path? = null,
        resourcePrefix: String? = null,
    ): File
}
