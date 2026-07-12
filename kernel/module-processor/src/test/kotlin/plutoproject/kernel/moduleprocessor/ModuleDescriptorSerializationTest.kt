package plutoproject.kernel.moduleprocessor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlinx.serialization.encodeToString
import plutoproject.kernel.api.ModuleDescriptor
import plutoproject.kernel.api.ModuleType
import plutoproject.kernel.api.Platform

class ModuleDescriptorSerializationTest {
    @Test
    fun `writes schema one descriptor with explicit dependency fields`() {
        val descriptor = ModuleDescriptor(
            id = "home",
            type = ModuleType.FEATURE,
            platform = Platform.PAPER,
            entrypoint = "example.\"Home\\Feature",
            requiredFeatures = listOf("teleport"),
            optionalFeatures = listOf("menu"),
            requiredCapabilities = listOf("mongo"),
        )

        assertEquals(
            """{"schemaVersion":1,"id":"home","type":"FEATURE","platform":"PAPER","entrypoint":"example.\"Home\\Feature","requiredFeatures":["teleport"],"optionalFeatures":["menu"],"requiredCapabilities":["mongo"]}""",
            moduleDescriptorJson.encodeToString(descriptor),
        )
    }
}
