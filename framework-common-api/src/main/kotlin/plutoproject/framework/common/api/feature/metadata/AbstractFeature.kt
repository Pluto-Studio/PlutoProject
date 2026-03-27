package plutoproject.framework.common.api.feature.metadata

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import plutoproject.framework.common.api.feature.Feature
import plutoproject.framework.common.api.feature.State
import plutoproject.framework.common.util.coroutine.PluginScope
import plutoproject.framework.common.util.coroutine.createSupervisorChild
import plutoproject.framework.common.util.jvm.extractFileFromJar
import java.io.File
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.Path

abstract class AbstractFeature : Feature {
    final override lateinit var id: String private set
    final override var state: State = State.UNINITIALIZED
        private set
    final override lateinit var logger: Logger private set
    final override lateinit var dataFolder: File private set
    final override lateinit var coroutineScope: CoroutineScope

    fun init(
        id: String,
        logger: Logger,
        dataFolder: File,
    ) {
        this.id = id
        this.logger = logger
        this.dataFolder = dataFolder
        this.state = State.INITIALIZED
    }

    fun updateState(newState: State) {
        state = newState
    }

    fun ensureCoroutineScopeActive() {
        if (::coroutineScope.isInitialized && coroutineScope.isActive) {
            return
        }
        coroutineScope = PluginScope.createSupervisorChild()
    }

    override fun saveConfig(resourcePrefix: String?): File {
        return saveResource("config.conf", resourcePrefix = resourcePrefix)
    }

    override fun saveResource(path: String, output: Path?, resourcePrefix: String?): File {
        val outputPath = dataFolder.toPath().resolve(output ?: Path(path))
        val outputFile = outputPath.toFile()
        if (outputFile.exists()) {
            return outputFile
        }
        return extractFileFromJar("${resourcePrefix ?: resourcePrefixInJar}/$path", outputPath)
    }
}
