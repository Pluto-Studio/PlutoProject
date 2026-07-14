package plutoproject.kernel.common

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ModuleResourceSaverTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `default namespace saves under module data folder and preserves existing file`() {
        val saver = saver(mapOf("module/mongo/config.conf" to "first"))
        val output = saver.save("config.conf", Path.of("nested/config.conf"), null, replace = false)

        assertEquals(tempDir.resolve("nested/config.conf"), output)
        assertEquals("first", Files.readString(output))

        Files.writeString(output, "user value")
        saver.save("config.conf", Path.of("nested/config.conf"), null, replace = false)
        assertEquals("user value", Files.readString(output))
    }

    @Test
    fun `custom prefix and absolute output are supported`() {
        val saver = saver(mapOf("defaults/mongo.conf" to "custom"))
        val output = tempDir.resolveSibling("absolute-output.conf")

        try {
            assertEquals(output, saver.save("mongo.conf", output, "defaults", replace = false))
            assertEquals("custom", Files.readString(output))
        } finally {
            Files.deleteIfExists(output)
        }
    }

    @Test
    fun `replace overwrites an existing file`() {
        val output = tempDir.resolve("config.conf")
        Files.writeString(output, "old")
        saver(mapOf("module/mongo/config.conf" to "new"))
            .save("config.conf", output, null, replace = true)
        assertEquals("new", Files.readString(output))
    }

    @Test
    fun `missing and invalid resource paths fail with context`() {
        val saver = saver(emptyMap())
        val missing = assertThrows(IllegalStateException::class.java) {
            saver.save("config.conf", Path.of("config.conf"), null, replace = false)
        }
        assertEquals(
            "Runtime module 'mongo' is missing packaged resource 'module/mongo/config.conf'",
            missing.message,
        )
        assertThrows(IllegalArgumentException::class.java) {
            saver.save("../config.conf", Path.of("config.conf"), null, replace = false)
        }
        assertThrows(IllegalArgumentException::class.java) {
            saver.save("config.conf", Path.of("config.conf"), "", replace = false)
        }
    }

    private fun saver(resources: Map<String, String>) = ModuleResourceSaver(
        moduleId = "mongo",
        dataFolder = tempDir,
        classLoader = ResourceClassLoader(resources),
    )
}

private class ResourceClassLoader(private val resources: Map<String, String>) : ClassLoader(null) {
    override fun getResourceAsStream(name: String): InputStream? =
        resources[name]?.toByteArray()?.let(::ByteArrayInputStream)
}
