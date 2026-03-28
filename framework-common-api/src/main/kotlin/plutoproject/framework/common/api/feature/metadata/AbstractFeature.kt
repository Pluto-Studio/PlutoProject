package plutoproject.framework.common.api.feature.metadata

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.koinApplication
import plutoproject.framework.common.api.feature.Feature
import plutoproject.framework.common.api.feature.FeatureCancellationException
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
    final override lateinit var coroutineScope: CoroutineScope private set
    final override val koin: Koin get() = koinApplication.koin
    private lateinit var koinApplication: KoinApplication

    fun init(
        id: String,
        logger: Logger,
        dataFolder: File,
    ) {
        this.id = id
        this.logger = logger
        this.dataFolder = dataFolder
        this.state = State.INITIALIZED
        this.koinApplication = koinApplication()
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

    fun cancelCoroutineScope() {
        coroutineScope.cancel(FeatureCancellationException(id))
    }

    // Koin 内部没有是否已关闭的标志，只是清空集合。
    // 所以其实关闭之后还是可以接着用的，不用像 CoroutineScope 那样在重新 enable 的时候重建。
    fun closeKoinApplication() {
        koinApplication.close()
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

    override fun koin(declaration: KoinAppDeclaration) {
        koinApplication.apply(declaration)
        koinApplication.createEagerInstances()
    }
}
