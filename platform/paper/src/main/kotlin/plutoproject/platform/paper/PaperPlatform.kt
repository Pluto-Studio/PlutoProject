package plutoproject.platform.paper

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import io.github.classgraph.ClassGraph
import kotlinx.coroutines.runBlocking
import plutoproject.kernel.common.PreparedRuntimeModules
import plutoproject.kernel.paper.PaperKernel
import plutoproject.platform.common.PLUTOPROJECT_CONSOLE_BANNER
import plutoproject.platform.common.PlatformConfig

@Suppress("UNUSED")
class PaperPlatform(
    private val prepared: PreparedRuntimeModules,
) : SuspendingJavaPlugin() {
    private lateinit var kernel: PaperKernel

    override fun onLoad() {
        logger.info("\n$PLUTOPROJECT_CONSOLE_BANNER")
        dataFolder.mkdirs()
        server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")
        preloadClasses()
        kernel = PaperKernel(
            plugin = this,
            dataFolder = dataFolder.toPath(),
            prepared = prepared,
        )
        runBlocking { kernel.load() }
    }

    fun preloadClasses() {
        val pluginClassLoader = PlatformConfig::class.java.classLoader
        loadClassesInPackages(
            "androidx",
            "cafe.adriel.voyager",
            classLoader = pluginClassLoader
        )
    }

    private fun loadClassesInPackages(
        vararg packageName: String,
        classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
    ) = ClassGraph()
        .acceptPackages(*packageName)
        .scan().use { result ->
            result.allClasses.forEach {
                runCatching {
                    classLoader.loadClass(it.name)
                }
            }
        }

    override fun onEnable() {
        runBlocking { kernel.enable() }
    }

    override fun onDisable() {
        runBlocking { kernel.shutdown() }
    }
}
