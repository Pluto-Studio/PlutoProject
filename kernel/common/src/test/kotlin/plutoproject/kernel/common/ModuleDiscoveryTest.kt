package plutoproject.kernel.common

import java.net.URLClassLoader
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import plutoproject.kernel.api.ModuleType
import plutoproject.kernel.api.Platform

class ModuleDiscoveryTest {
    @TempDir
    lateinit var resources: Path

    @Test
    fun `discovers platform descriptors and retains malformed source errors`() {
        val directory = resources.resolve("META-INF/plutoproject/modules/paper").apply { createDirectories() }
        directory.resolve("home.json").writeText(
            """{"schemaVersion":1,"id":"home","type":"FEATURE","platform":"PAPER","entrypoint":"fixture.Home","requiredFeatures":[],"optionalFeatures":["menu"],"requiredCapabilities":["mongo"],"futureField":true}""",
        )
        directory.resolve("broken.json").writeText("{not-json")
        directory.resolve("future.json").writeText(
            """{"schemaVersion":99,"id":"future","type":"FEATURE","platform":"PAPER","entrypoint":"fixture.Future"}""",
        )
        URLClassLoader(arrayOf(resources.toUri().toURL()), null).use { classLoader ->
            val result = ModuleDiscovery(classLoader).discover(Platform.PAPER)

            assertEquals(1, result.modules.size)
            assertEquals("home", result.modules.single().descriptor.id)
            assertEquals(ModuleType.FEATURE, result.modules.single().descriptor.type)
            assertEquals(listOf("menu"), result.modules.single().descriptor.optionalFeatures)
            assertEquals(2, result.errors.size)
            assertTrue(result.errors.any { it.source.endsWith("broken.json") })
            assertTrue(result.errors.any { it.source.endsWith("future.json") && "schema 99" in (it.cause?.message ?: "") })
        }
    }
}
