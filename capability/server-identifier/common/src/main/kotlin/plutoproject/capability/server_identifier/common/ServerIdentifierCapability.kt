package plutoproject.capability.server_identifier.common

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import java.nio.file.Files
import java.nio.file.Path
import plutoproject.capability.server_identifier.api.ServerIdentifier
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.RuntimeModule
import plutoproject.kernel.api.exportService

private const val SYSTEM_PROPERTY = "plutoproject.serverIdentifier"
private const val ENVIRONMENT_VARIABLE = "PLUTOPROJECT_SERVER_IDENTIFIER"
private val VALID_IDENTIFIER = Regex("[A-Za-z0-9_\\-$]+")

class ServerIdentifierCapability : RuntimeModule {
    override suspend fun onLoad(context: ModuleContext) {
        val identifier = resolveServerIdentifier(
            systemProperty = { System.getProperty(SYSTEM_PROPERTY) },
            environmentVariable = { System.getenv(ENVIRONMENT_VARIABLE) },
            configFile = context.dataFolder.resolve("config.conf"),
        )
        context.services.exportService<ServerIdentifier>(DefaultServerIdentifier(identifier))
    }
}

internal data class ServerIdentifierConfig(
    val serverIdentifier: String? = null,
)

internal fun resolveServerIdentifier(
    systemProperty: () -> String?,
    environmentVariable: () -> String?,
    configFile: Path,
): String? {
    val identifier = systemProperty()
        ?: environmentVariable()
        ?: readIdentifier(configFile)
    require(identifier == null || VALID_IDENTIFIER.matches(identifier)) {
        "Server identifier contains invalid characters"
    }
    return identifier
}

private fun readIdentifier(configFile: Path): String? {
    if (Files.notExists(configFile)) return null
    return ConfigLoaderBuilder.empty()
        .addDefaults()
        .addParser("conf", HoconParser())
        .addPropertySource(PropertySource.file(configFile.toFile()))
        .build()
        .loadConfigOrThrow<ServerIdentifierConfig>()
        .serverIdentifier
}

private class DefaultServerIdentifier(
    override val identifier: String?,
) : ServerIdentifier
