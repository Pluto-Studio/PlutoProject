package plutoproject.framework.common.api.feature.metadata

import plutoproject.framework.common.api.feature.Feature
import plutoproject.framework.common.api.feature.State
import plutoproject.framework.common.util.jvm.extractFileFromJar
import java.io.File
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.Path

abstract class AbstractFeature<S : Any, P : Any> : Feature<S, P> {
    final override lateinit var id: String private set
    final override var state: State = State.UNINITIALIZED
        private set
    final override lateinit var server: S private set
    final override lateinit var plugin: P private set
    final override lateinit var logger: Logger private set
    final override lateinit var dataFolder: File private set

    fun init(
        id: String,
        server: S,
        plugin: P,
        logger: Logger,
        dataFolder: File,
    ) {
        this.id = id
        this.server = server
        this.plugin = plugin
        this.logger = logger
        this.dataFolder = dataFolder
        this.state = State.INITIALIZED
    }

    fun updateState(newState: State) {
        state = newState
    }

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
