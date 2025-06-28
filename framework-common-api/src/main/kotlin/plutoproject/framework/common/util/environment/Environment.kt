package plutoproject.framework.common.util.environment

import plutoproject.framework.common.PlutoConfig
import plutoproject.framework.common.util.PlatformType
import plutoproject.framework.common.util.inject.Koin
import java.io.File
import java.time.Instant
import java.util.jar.Manifest

lateinit var platformType: PlatformType
lateinit var serverThread: Thread
lateinit var pluginDataFolder: File
lateinit var featureDataFolder: File
lateinit var frameworkDataFolder: File

val serverName: String
    get() = Koin.get<PlutoConfig>().serverName

fun readManifestAttribute(attribute: String): String {
    val manifestStream = PlutoConfig::class.java.classLoader
        .getResourceAsStream("META-INF/MANIFEST.MF") ?: error("Missing manifest file")
    val manifest = Manifest(manifestStream)
    return manifest.mainAttributes.getValue(attribute)
}

val plutoProjectVersion: String = readManifestAttribute("PlutoProject-Version")
val plutoProjectReleaseName: String = readManifestAttribute("PlutoProject-Release-Name")
val plutoProjectReleaseChannel: ReleaseChannel =
    ReleaseChannel.valueOf(readManifestAttribute("PlutoProject-Release-Channel"))
val plutoProjectGitCommit: String = readManifestAttribute("PlutoProject-Git-Commit")
val plutoProjectGitBranch: String = readManifestAttribute("PlutoProject-Git-Branch")
val plutoProjectBuildTime: Instant = Instant.ofEpochMilli(readManifestAttribute("PlutoProject-Build-Time").toLong())

fun getFrameworkModuleDataFolder(id: String) = frameworkDataFolder.resolve(id).also { it.mkdirs() }

fun getFeatureDataFolder(id: String) = featureDataFolder.resolve(id).also { it.mkdirs() }

fun File.initPluginDataFolder() {
    pluginDataFolder = this.also { it.mkdirs() }
    featureDataFolder = pluginDataFolder.resolve("feature${File.separator}").also { it.mkdirs() }
    frameworkDataFolder = pluginDataFolder.resolve("framework${File.separator}").also { it.mkdirs() }
}
