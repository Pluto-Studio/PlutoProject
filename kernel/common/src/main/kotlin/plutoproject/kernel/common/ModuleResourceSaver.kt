package plutoproject.kernel.common

import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class ModuleResourceSaver(
    private val moduleId: String,
    private val dataFolder: Path,
    private val classLoader: ClassLoader,
) {
    fun save(path: String, output: Path, resourcePrefix: String?, replace: Boolean): Path {
        val normalizedPath = normalizeResourcePath(path, "resource path")
        val normalizedPrefix = normalizeResourcePath(resourcePrefix ?: "module/$moduleId", "resource prefix")
        val outputPath = if (output.isAbsolute) output.normalize() else dataFolder.resolve(output).normalize()
        if (!replace && Files.exists(outputPath)) return outputPath

        val resourcePath = "$normalizedPrefix/$normalizedPath"
        val source = classLoader.getResourceAsStream(resourcePath)
            ?: error("Runtime module '$moduleId' is missing packaged resource '$resourcePath'")
        Files.createDirectories(requireNotNull(outputPath.parent) { "Output path '$outputPath' has no parent" })
        val options: Array<CopyOption> = if (replace) arrayOf(StandardCopyOption.REPLACE_EXISTING) else emptyArray()
        source.use { Files.copy(it, outputPath, *options) }
        return outputPath
    }

    private fun normalizeResourcePath(value: String, label: String): String {
        val segments = value.replace('\\', '/').trim('/').split('/').filter { it.isNotEmpty() && it != "." }
        require(segments.isNotEmpty()) { "$label must not be empty" }
        require(".." !in segments) { "$label must not contain '..'" }
        return segments.joinToString("/")
    }
}
