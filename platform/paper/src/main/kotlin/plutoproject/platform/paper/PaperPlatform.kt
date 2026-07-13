package plutoproject.platform.paper

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import io.github.classgraph.ClassGraph
import kotlinx.coroutines.runBlocking
import plutoproject.kernel.paper.PaperKernel
import plutoproject.platform.common.PlatformConfig
import plutoproject.platform.common.resolvePlatformConfig
import java.io.File

@Suppress("UNUSED")
class PaperPlatform : SuspendingJavaPlugin() {
    private lateinit var kernel: PaperKernel

    override fun onLoad() {
        dataFolder.mkdirs()
        dataFolder.resolve("module${File.separator}")
        server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")
        saveResource("config.conf", false)
        preloadClasses()
        kernel = PaperKernel(
            plugin = this,
            dataFolder = dataFolder.toPath(),
            featureRoots = resolvePlatformConfig(dataFolder.toPath().resolve("config.conf")).enableFeatures,
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
        loadClassesInPackages(
            "plutoproject.framework.common",
            "plutoproject.framework.paper",
            "plutoproject.feature.common",
            "plutoproject.feature.paper",
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
