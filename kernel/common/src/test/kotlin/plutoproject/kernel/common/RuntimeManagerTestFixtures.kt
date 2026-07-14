package plutoproject.kernel.common

import java.nio.file.Path
import java.util.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import org.koin.core.Koin
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.ModuleDescriptor
import plutoproject.kernel.api.ModuleServices
import plutoproject.kernel.api.ModuleType
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule

internal fun feature(
    id: String,
    requiredFeatures: List<String> = emptyList(),
    optionalFeatures: List<String> = emptyList(),
    requiredCapabilities: List<String> = emptyList(),
    platform: Platform = Platform.PAPER,
) = ModuleDescriptor(
    id = id,
    type = ModuleType.FEATURE,
    platform = platform,
    entrypoint = "fixture.$id",
    requiredFeatures = requiredFeatures,
    optionalFeatures = optionalFeatures,
    requiredCapabilities = requiredCapabilities,
)

internal fun capability(
    id: String,
    requiredCapabilities: List<String> = emptyList(),
    platform: Platform = Platform.PAPER,
) = ModuleDescriptor(
    id = id,
    type = ModuleType.CAPABILITY,
    platform = platform,
    entrypoint = "fixture.$id",
    requiredCapabilities = requiredCapabilities,
)

internal class TestModule(
    private val id: String,
    private val events: MutableList<String>,
    private val failLoad: Boolean = false,
    private val failEnable: Boolean = false,
    private val failDisable: Boolean = false,
) : RuntimeModule {
    override suspend fun onLoad(context: ModuleContext) {
        events += "$id.load"
        if (failLoad) error("$id load failure")
    }

    override suspend fun onEnable(context: ModuleContext) {
        events += "$id.enable"
        if (failEnable) error("$id enable failure")
    }

    override suspend fun onDisable(context: ModuleContext) {
        events += "$id.disable"
        if (failDisable) error("$id disable failure")
    }
}

internal class TestContext(
    override val id: String,
    override val koin: Koin,
    override val services: ModuleServices,
) : ModuleContext {
    override val logger: Logger = Logger.getLogger("test.$id")
    override val dataFolder: Path = Path.of("build/test-data", id)
    override val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob())

    override fun saveResource(path: String, output: Path, resourcePrefix: String?, replace: Boolean): Path =
        error("Resource saving is not available in lifecycle test contexts")

    val job: Job get() = coroutineScope.coroutineContext[Job]!!
}

internal class ManagerFixture(
    descriptors: List<ModuleDescriptor>,
    roots: List<String>,
    failures: Map<String, String> = emptyMap(),
) {
    val events = mutableListOf<String>()
    val contexts = mutableMapOf<String, TestContext>()
    val creations = mutableMapOf<String, Int>()
    val manager = RuntimeModuleManager(
        platform = Platform.PAPER,
        descriptors = descriptors,
        featureRoots = roots,
        moduleFactory = RuntimeModuleFactory { descriptor ->
            creations.compute(descriptor.id) { _, count -> (count ?: 0) + 1 }
            TestModule(
                descriptor.id,
                events,
                failLoad = failures[descriptor.id] == "load",
                failEnable = failures[descriptor.id] == "enable",
                failDisable = failures[descriptor.id] == "disable",
            )
        },
        contextFactory = ModuleContextFactory { descriptor, koin, services ->
            TestContext(descriptor.id, koin, services).also { contexts[descriptor.id] = it }
        },
    )

    suspend fun start() {
        manager.loadStartup()
        manager.enableStartup()
    }
}
