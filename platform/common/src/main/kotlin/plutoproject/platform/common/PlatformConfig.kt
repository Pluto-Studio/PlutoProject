package plutoproject.platform.common

import com.typesafe.config.ConfigFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.hocon.decodeFromConfig
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.pathString

@Serializable
data class PlatformConfig(
    val enableFeatures: List<String> = listOf()
)

@OptIn(ExperimentalSerializationApi::class)
fun resolvePlatformConfig(path: Path): PlatformConfig {
    require(path.exists()) { "Platform configuration file doesn't exist at `${path.pathString}`" }
    return Hocon.decodeFromConfig(ConfigFactory.parseFile(path.toFile()).resolve())
}
