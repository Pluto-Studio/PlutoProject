package plutoproject.feature.whitelist_v2.infra.mongo

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import kotlinx.coroutines.test.runTest
import org.bson.UuidRepresentation
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import plutoproject.feature.whitelist_v2.api.WhitelistOperator
import plutoproject.feature.whitelist_v2.core.VisitorRecordData
import plutoproject.feature.whitelist_v2.core.WhitelistRecordData
import plutoproject.feature.whitelist_v2.infra.mongo.model.VisitorRecordDocument
import plutoproject.feature.whitelist_v2.infra.mongo.model.WhitelistRecordDocument
import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Instant
import java.util.*
import kotlin.time.Duration.Companion.seconds

@Testcontainers(disabledWithoutDocker = true)
class MongoWhitelistRepositoriesTest {
    @Container
    private val mongo = MongoDBContainer("mongo:7.0.14")

    @Test
    fun `whitelist record should be saved and loaded`() = runTest {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable)

        val client = MongoClient.create(
            MongoClientSettings.builder()
                .applyConnectionString(ConnectionString(mongo.replicaSetUrl))
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .build()
        )
        val db = client.getDatabase("test")

        val collection = db.getCollection<WhitelistRecordDocument>("whitelist_records")
        val repo = MongoWhitelistRecordRepository(collection)

        val uid = UUID.fromString("00000000-0000-0000-0000-000000000101")
        val record = WhitelistRecordData(
            uniqueId = uid,
            username = "abc",
            granter = WhitelistOperator.Console,
            createdAt = Instant.parse("2026-02-07T00:00:00Z"),
            joinedAsVisitorBefore = true,
            isMigrated = false,
            isRevoked = false,
            revoker = null,
            revokeReason = null,
            revokeAt = null,
        )
        repo.saveOrUpdate(record)

        val loaded = repo.findByUniqueId(uid)
        assertNotNull(loaded)
        loaded!!
        assertEquals("abc", loaded.username)
        assertTrue(loaded.joinedAsVisitorBefore)
        assertFalse(loaded.isRevoked)
    }

    @Test
    fun `visitor record should be queryable by ip`() = runTest {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable)

        val client = MongoClient.create(
            MongoClientSettings.builder()
                .applyConnectionString(ConnectionString(mongo.replicaSetUrl))
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .build()
        )
        val db = client.getDatabase("test")

        val collection = db.getCollection<VisitorRecordDocument>("visitor_records")
        val repo = MongoVisitorRecordRepository(collection)
        repo.ensureIndexes()

        val uid = UUID.fromString("00000000-0000-0000-0000-000000000201")
        val ip = InetAddress.getByName("127.0.0.1")
        val record = VisitorRecordData(
            uniqueId = uid,
            ipAddress = ip,
            virtualHost = InetSocketAddress("localhost", 25565),
            visitedAt = Instant.parse("2026-02-07T00:00:00Z"),
            createdAt = Instant.parse("2026-02-07T00:00:05Z"),
            duration = 5.seconds,
            visitedServers = listOf("lobby"),
        )
        repo.save(record)

        val byIp = repo.findByIpAddress(ip)
        assertEquals(1, byIp.size)
        assertEquals(uid, byIp.single().uniqueId)
    }
}
