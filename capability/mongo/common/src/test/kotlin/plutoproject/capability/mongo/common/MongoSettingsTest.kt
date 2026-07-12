package plutoproject.capability.mongo.common

import org.bson.UuidRepresentation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MongoSettingsTest {
    @Test
    fun `settings preserve endpoint database credentials and UUID representation`() {
        val settings = settings(
            MongoConfig(
                host = "mongo.internal",
                port = 27018,
                database = "pluto",
                username = "user@example.com",
                password = "p@ss word",
            ),
        )

        assertEquals("mongo.internal", settings.clusterSettings.hosts.single().host)
        assertEquals(27018, settings.clusterSettings.hosts.single().port)
        assertEquals("user@example.com", settings.credential?.userName)
        assertEquals("pluto", settings.credential?.source)
        assertEquals(UuidRepresentation.STANDARD, settings.uuidRepresentation)
    }
}
