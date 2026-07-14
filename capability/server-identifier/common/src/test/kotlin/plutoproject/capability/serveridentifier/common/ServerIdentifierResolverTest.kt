package plutoproject.capability.serveridentifier.common

import java.nio.file.Path
import kotlin.io.path.createTempFile
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ServerIdentifierResolverTest {
    @Test
    fun `server identifier is read from JVM property`() {
        assertEquals("paper-1", resolve(systemProperty = "paper-1"))
    }

    @Test
    fun `server identifier is read from environment variable`() {
        assertEquals("velocity_1", resolve(environmentVariable = "velocity_1"))
    }

    @Test
    fun `server identifier is read from config file`() {
        assertEquals("lobby$", resolve(configIdentifier = "lobby$"))
    }

    @Test
    fun `server identifier sources follow priority order`() {
        assertEquals(
            "jvm",
            resolve(
                systemProperty = "jvm",
                environmentVariable = "environment",
                configIdentifier = "config",
            ),
        )
        assertEquals(
            "environment",
            resolve(environmentVariable = "environment", configIdentifier = "config"),
        )
    }

    @Test
    fun `server identifier is absent when no source provides one`() {
        assertNull(resolve())
    }

    @ParameterizedTest
    @ValueSource(strings = ["with space", "中文", "server.name", "server/name", "server@name", ""])
    fun `server identifier rejects unsupported characters`(identifier: String) {
        assertThrows(IllegalArgumentException::class.java) {
            resolve(systemProperty = identifier)
        }
    }

    private fun resolve(
        systemProperty: String? = null,
        environmentVariable: String? = null,
        configIdentifier: String? = null,
    ): String? {
        val configFile = configIdentifier?.let {
            createTempFile(suffix = ".conf").apply { writeText("server-identifier = \"$it\"") }
        } ?: Path.of("missing-config.conf")
        return resolveServerIdentifier(
            systemProperty = { systemProperty },
            environmentVariable = { environmentVariable },
            configFile = configFile,
        )
    }
}
