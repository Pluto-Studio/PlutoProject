package plutoproject.kernel.api

import kotlinx.serialization.Serializable

const val MODULE_DESCRIPTOR_SCHEMA_VERSION = 1

@Serializable
data class ModuleDescriptor(
    val schemaVersion: Int = MODULE_DESCRIPTOR_SCHEMA_VERSION,
    val id: String,
    val type: ModuleType,
    val platform: Platform,
    val entrypoint: String,
    val requiredFeatures: List<String> = emptyList(),
    val optionalFeatures: List<String> = emptyList(),
    val requiredCapabilities: List<String> = emptyList(),
)

@Serializable
enum class ModuleType {
    FEATURE,
    CAPABILITY,
}

@Serializable
enum class Platform(val resourceDirectory: String) {
    PAPER("paper"),
    VELOCITY("velocity"),
}
