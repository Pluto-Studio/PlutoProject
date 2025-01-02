package plutoproject.framework.common.api.feature

import plutoproject.framework.common.util.jvm.extractFileFromJar
import java.io.File
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.Path

abstract class AbstractFeature<S : Any, P : Any> : Feature<S, P> {
    override lateinit var server: S
    override lateinit var plugin: P
    override lateinit var logger: Logger
    override lateinit var dataFolder: File
    override lateinit var resourcePrefixInJar: String

    override fun saveConfig(resourcePrefix: String?): File =
        extractFileFromJar(
            "${resourcePrefix ?: resourcePrefixInJar}/config.conf",
            dataFolder.toPath().resolve("config.conf")
        )

    override fun saveResource(path: String, outputPath: Path?, resourcePrefix: String?): File =
        extractFileFromJar(
            "${resourcePrefix ?: resourcePrefixInJar}/$path",
            dataFolder.toPath().resolve(outputPath ?: Path(path))
        )
}
