package plutoproject.kernel.common

import java.net.JarURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarFile
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import plutoproject.kernel.api.MODULE_DESCRIPTOR_SCHEMA_VERSION
import plutoproject.kernel.api.ModuleDescriptor
import plutoproject.kernel.api.ModuleType
import plutoproject.kernel.api.Platform

data class DiscoveredModule(
    val descriptor: ModuleDescriptor,
    val source: String,
)

data class ModuleDiscoveryResult(
    val modules: List<DiscoveredModule>,
    val errors: List<ModuleDiscoveryError>,
)

data class ModuleDiscoveryError(
    val source: String,
    val message: String,
    val cause: Throwable? = null,
)

class ModuleDiscovery(
    private val classLoader: ClassLoader = ModuleDiscovery::class.java.classLoader,
) {
    private val descriptorJson = Json { ignoreUnknownKeys = true }

    fun discover(platform: Platform): ModuleDiscoveryResult {
        val prefix = "META-INF/plutoproject/modules/${platform.resourceDirectory}"
        val resources = linkedMapOf<String, String>()
        val errors = mutableListOf<ModuleDiscoveryError>()
        classLoader.getResources(prefix).toList().forEach { url ->
            runCatching { readDirectory(url, prefix, resources) }
                .onFailure { errors += ModuleDiscoveryError(url.toString(), "Unable to scan descriptor directory", it) }
        }
        val modules = resources.mapNotNull { (source, content) ->
            runCatching {
                val descriptor = parseDescriptor(content)
                require(descriptor.schemaVersion == MODULE_DESCRIPTOR_SCHEMA_VERSION) {
                    "Unsupported descriptor schema ${descriptor.schemaVersion}"
                }
                DiscoveredModule(descriptor, source)
            }
                .onFailure { errors += ModuleDiscoveryError(source, "Malformed runtime module descriptor", it) }
                .getOrNull()
        }
        return ModuleDiscoveryResult(modules, errors)
    }

    private fun readDirectory(url: URL, prefix: String, output: MutableMap<String, String>) {
        when (url.protocol) {
            "file" -> readFileDirectory(Paths.get(url.toURI()), output)
            "jar" -> {
                val connection = url.openConnection() as JarURLConnection
                readJar(connection.jarFile, prefix, output, connection.jarFileURL.toString())
            }
            else -> error("Unsupported descriptor resource protocol '${url.protocol}'")
        }
    }

    private fun readFileDirectory(directory: Path, output: MutableMap<String, String>) {
        if (!Files.isDirectory(directory)) return
        Files.list(directory).use { files ->
            files.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".json") }
                .sorted()
                .forEach { output[it.toUri().toString()] = Files.readString(it) }
        }
    }

    private fun readJar(
        jar: JarFile,
        prefix: String,
        output: MutableMap<String, String>,
        jarSource: String,
    ) {
        jar.entries().asSequence()
            .filter { !it.isDirectory && it.name.startsWith("$prefix/") && it.name.endsWith(".json") }
            .sortedBy { it.name }
            .forEach { entry ->
                output["$jarSource!/${entry.name}"] = jar.getInputStream(entry).bufferedReader().use { it.readText() }
            }
    }

    private fun parseDescriptor(content: String): ModuleDescriptor {
        return descriptorJson.decodeFromString(content)
    }
}
