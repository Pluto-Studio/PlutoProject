package plutoproject.framework.common.api.feature

import java.io.File
import java.nio.file.Path

interface Feature<S : Any, P : Any> {
    var server: S
    val plugin: P
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
