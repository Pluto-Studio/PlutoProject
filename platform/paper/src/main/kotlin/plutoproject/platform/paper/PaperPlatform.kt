package plutoproject.platform.paper

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import io.github.classgraph.ClassGraph
import kotlinx.coroutines.runBlocking
import plutoproject.kernel.paper.PaperKernel
import plutoproject.platform.common.PLUTOPROJECT_CONSOLE_BANNER
import plutoproject.platform.common.PlatformConfig
import plutoproject.platform.common.resolvePlatformConfig
import java.io.File

@Suppress("UNUSED")
class PaperPlatform : SuspendingJavaPlugin() {
    private lateinit var kernel: PaperKernel

    override fun onLoad() {
        logger.info("\n$PLUTOPROJECT_CONSOLE_BANNER")
        dataFolder.mkdirs()
        dataFolder.resolve("module${File.separator}")
        server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")
        val configFile = dataFolder.resolve("config.conf")
        if (!configFile.exists()) {
            saveResource("config.conf", false)
        }
        preloadClasses()
        kernel = PaperKernel(
            plugin = this,
            dataFolder = dataFolder.toPath(),
            featureRoots = resolvePlatformConfig(configFile.toPath()).enableFeatures,
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
