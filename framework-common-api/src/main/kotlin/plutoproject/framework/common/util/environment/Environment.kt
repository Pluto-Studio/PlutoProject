package plutoproject.framework.common.util.environment

import plutoproject.framework.common.PlutoConfig
import plutoproject.framework.common.util.PlatformType
import plutoproject.framework.common.util.inject.Koin
import java.io.File
import java.net.JarURLConnection
import java.time.Instant
import java.util.jar.JarFile

lateinit var platformType: PlatformType
lateinit var serverThread: Thread
lateinit var pluginDataFolder: File
lateinit var featureDataFolder: File
lateinit var frameworkDataFolder: File

val serverName: String
    get() = Koin.get<PlutoConfig>().serverName

/*
* Velocity 里通过 PluginClassLoader 获取 Manifest 居然会获取到 Velocity 自身的。。。
* 这里判断一下是不是我们的 Jar。
*/
private fun readManifestAttribute(attribute: String): String {
    val identityFile = "plutoproject_jar_identity"
    val loader = ReleaseChannel::class.java.classLoader
    val resources = loader.getResources("META-INF/MANIFEST.MF")

    resources.asSequence().forEach { url ->
        val jarConnection = url.openConnection() as? JarURLConnection ?: return@forEach
        val jarFileUrl = jarConnection.jarFileURL
        JarFile(jarFileUrl.toURI().path).use { jar ->
            if (jar.getEntry(identityFile) == null) {
                return@forEach
            }
            jar.manifest?.mainAttributes?.let { attrs ->
                val value = attrs.getValue(attribute)
                if (value != null) {
                    return value
                } else {
                    error("Attribute '$attribute' not found in manifest of $jarFileUrl")
                }
            } ?: error("Manifest missing in $jarFileUrl")
        }
    }

    error("Cannot find manifest with identity file '$identityFile'")
}

val plutoProjectVersion: String = readManifestAttribute("PlutoProject-Version")
val plutoProjectReleaseName: String = readManifestAttribute("PlutoProject-Release-Name")
val plutoProjectReleaseChannel: ReleaseChannel =
    ReleaseChannel.valueOf(readManifestAttribute("PlutoProject-Release-Channel").uppercase())
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
