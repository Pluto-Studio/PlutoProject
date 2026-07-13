package plutoproject.kernel.common

import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
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
        resources.resolve("META-INF/plutoproject/module-packages.idx").writeText(
            "fixture.home\thome\nfixture.shared\tshared\n",
        )
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
            assertEquals(mapOf("fixture.home" to "home", "fixture.shared" to "shared"), result.packageOwners)
            assertEquals("home", result.modules.single().descriptor.id)
            assertEquals(ModuleType.FEATURE, result.modules.single().descriptor.type)
            assertEquals(listOf("menu"), result.modules.single().descriptor.optionalFeatures)
            assertEquals(2, result.errors.size)
            assertTrue(result.errors.any { it.source.endsWith("broken.json") })
            assertTrue(result.errors.any { it.source.endsWith("future.json") && "schema 99" in (it.cause?.message ?: "") })
        }
    }

    @Test
    fun `discovers descriptors and package index from a jar`() {
        val jar = resources.resolve("modules.jar")
        JarOutputStream(Files.newOutputStream(jar)).use { output ->
            output.putNextEntry(JarEntry("META-INF/"))
            output.closeEntry()
            output.putNextEntry(JarEntry("META-INF/plutoproject/"))
            output.closeEntry()
            output.putNextEntry(JarEntry("META-INF/plutoproject/modules/"))
            output.closeEntry()
            output.putNextEntry(JarEntry("META-INF/plutoproject/modules/paper/"))
            output.closeEntry()
            output.putNextEntry(JarEntry("META-INF/plutoproject/module-packages.idx"))
            output.write("fixture.jar\tjar_module\n".toByteArray())
            output.closeEntry()
            output.putNextEntry(JarEntry("META-INF/plutoproject/modules/paper/jar_module.json"))
            output.write(
                """{"schemaVersion":1,"id":"jar_module","type":"FEATURE","platform":"PAPER","entrypoint":"fixture.JarModule"}""".toByteArray(),
            )
            output.closeEntry()
        }

        URLClassLoader(arrayOf(jar.toUri().toURL()), null).use { classLoader ->
            val result = ModuleDiscovery(classLoader).discover(Platform.PAPER)

            assertEquals(listOf("jar_module"), result.modules.map { it.descriptor.id })
            assertEquals(mapOf("fixture.jar" to "jar_module"), result.packageOwners)
            assertTrue(result.errors.isEmpty())
        }
    }
}
