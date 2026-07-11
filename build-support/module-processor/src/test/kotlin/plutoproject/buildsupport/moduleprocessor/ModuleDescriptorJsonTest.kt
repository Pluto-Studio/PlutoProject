package plutoproject.buildsupport.moduleprocessor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import plutoproject.kernel.api.ModuleDescriptor
import plutoproject.kernel.api.ModuleType
import plutoproject.kernel.api.Platform

class ModuleDescriptorJsonTest {
    @Test
    fun `writes schema one descriptor with explicit dependency fields`() {
        val descriptor = ModuleDescriptor(
            id = "home",
            type = ModuleType.FEATURE,
            platform = Platform.PAPER,
            entrypoint = "example.HomeFeature",
            requiredFeatures = listOf("teleport"),
            optionalFeatures = listOf("menu"),
            requiredCapabilities = listOf("mongo"),
        )

        assertEquals(
            """{"schemaVersion":1,"id":"home","type":"FEATURE","platform":"PAPER","entrypoint":"example.HomeFeature","requiredFeatures":["teleport"],"optionalFeatures":["menu"],"requiredCapabilities":["mongo"]}""",
            ModuleDescriptorJson.encode(descriptor),
        )
    }
}
