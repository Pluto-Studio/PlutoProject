package plutoproject.framework.common.util

import plutoproject.framework.common.PlutoConfig
import plutoproject.framework.common.util.inject.Koin
import java.io.File

lateinit var platformType: PlatformType
lateinit var serverThread: Thread
lateinit var pluginDataFolder: File
lateinit var featureDataFolder: File
lateinit var frameworkDataFolder: File

val serverName: String
    get() = Koin.get<PlutoConfig>().serverName

fun getFrameworkModuleDataFolder(id: String) = frameworkDataFolder.resolve(id).also { it.mkdirs() }

fun getFeatureDataFolder(id: String) = featureDataFolder.resolve(id).also { it.mkdirs() }

fun File.initPluginDataFolder() {
    pluginDataFolder = this.also { it.mkdirs() }
    featureDataFolder = pluginDataFolder.resolve("feature${File.separator}").also { it.mkdirs() }
    frameworkDataFolder = pluginDataFolder.resolve("framework${File.separator}").also { it.mkdirs() }
}
