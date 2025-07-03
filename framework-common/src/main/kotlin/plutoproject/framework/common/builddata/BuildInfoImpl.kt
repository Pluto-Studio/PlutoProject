package plutoproject.framework.common.builddata

import plutoproject.framework.common.util.buildinfo.BuildInfo
import plutoproject.framework.common.util.buildinfo.ReleaseChannel
import java.net.JarURLConnection
import java.time.Instant
import java.util.jar.JarFile

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

class BuildInfoImpl : BuildInfo {
    override val version: String = readManifestAttribute("PlutoProject-Version")
    override val releaseName: String = readManifestAttribute("PlutoProject-Release-Name")
    override val releaseChannel: ReleaseChannel =
        ReleaseChannel.valueOf(readManifestAttribute("PlutoProject-Release-Channel").uppercase())
    override val gitCommit: String = readManifestAttribute("PlutoProject-Git-Commit")
    override val gitBranch: String = readManifestAttribute("PlutoProject-Git-Branch")
    override val buildTime: Instant = Instant.ofEpochMilli(readManifestAttribute("PlutoProject-Build-Time").toLong())
}
