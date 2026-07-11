package plutoproject.kernel.common

import java.net.JarURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarFile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
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
        val json = Json.parseToJsonElement(content) as? JsonObject ?: error("Descriptor must be a JSON object")
        return ModuleDescriptor(
            schemaVersion = json.requiredPrimitive("schemaVersion").int,
            id = json.requiredPrimitive("id").content,
            type = ModuleType.valueOf(json.requiredPrimitive("type").content),
            platform = Platform.valueOf(json.requiredPrimitive("platform").content),
            entrypoint = json.requiredPrimitive("entrypoint").content,
            requiredFeatures = json.stringList("requiredFeatures"),
            optionalFeatures = json.stringList("optionalFeatures"),
            requiredCapabilities = json.stringList("requiredCapabilities"),
        )
    }

    private fun JsonObject.requiredPrimitive(name: String) =
        this[name] as? JsonPrimitive ?: error("Missing or invalid '$name'")

    private fun JsonObject.stringList(name: String): List<String> {
        val value = this[name] ?: return emptyList()
        return (value as? JsonArray)?.map {
            (it as? JsonPrimitive)?.content ?: error("'$name' must contain only strings")
        } ?: error("'$name' must be an array")
    }
}
