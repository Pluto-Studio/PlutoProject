package plutoproject.framework.common.util

import java.io.File

lateinit var serverThread: Thread
lateinit var pluginDataFolder: File
lateinit var featureDataFolder: File
lateinit var frameworkDataFolder: File

fun getFrameworkModuleDataFolder(id: String) = frameworkDataFolder.resolve(id).also { it.mkdirs() }

fun getFeatureDataFolder(id: String) = featureDataFolder.resolve(id).also { it.mkdirs() }

fun File.initPluginDataFolder() {
    pluginDataFolder = this.also { it.mkdirs() }
    featureDataFolder = pluginDataFolder.resolve("feature${File.separator}").also { it.mkdirs() }
    frameworkDataFolder = pluginDataFolder.resolve("framework${File.separator}").also { it.mkdirs() }
}
