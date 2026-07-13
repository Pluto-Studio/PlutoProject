package plutoproject.feature.dev_watermark.paper

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.text
import plutoproject.foundation.common.text.mochaMaroon
import plutoproject.foundation.common.text.mochaSubtext0
import plutoproject.foundation.common.text.mochaText
import java.net.JarURLConnection
import java.util.jar.JarFile

private fun readBuildAttribute(attribute: String): String {
    val resources = DevWatermarkFeature::class.java.classLoader.getResources("META-INF/MANIFEST.MF")
    resources.asSequence().forEach { url ->
        val connection = url.openConnection() as? JarURLConnection ?: return@forEach
        JarFile(connection.jarFileURL.toURI().path).use { jar ->
            if (jar.getEntry("plutoproject_jar_identity") == null) return@forEach
            return requireNotNull(jar.manifest.mainAttributes.getValue(attribute)) {
                "Attribute '$attribute' not found in ${connection.jarFileURL}"
            }
        }
    }
    error("Cannot find PlutoProject distribution manifest")
}

val DEV_WATERMARK = component {
    if (!readBuildAttribute("PlutoProject-Release-Channel").equals("stable", ignoreCase = true)) {
        text("开发版本，不代表最终品质") with mochaText
        text(" (${readBuildAttribute("PlutoProject-Git-Commit")}@${readBuildAttribute("PlutoProject-Git-Branch")})") with mochaSubtext0
    } else {
        text("开发版水印已启用，但未在运行开发版") with mochaMaroon
    }
}
