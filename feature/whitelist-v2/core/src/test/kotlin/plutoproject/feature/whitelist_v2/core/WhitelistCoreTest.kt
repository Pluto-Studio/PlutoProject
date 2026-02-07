package plutoproject.feature.whitelist_v2.core

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.whitelist_v2.api.VisitorRecordParams
import plutoproject.feature.whitelist_v2.api.WhitelistOperator
import plutoproject.feature.whitelist_v2.api.WhitelistRevokeReason
import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class WhitelistCoreTest {
    @Test
    fun `grantWhitelist returns false when already active`() = runBlocking {
        val clock = fixedClock("2026-02-07T00:00:00Z")
        val whitelistRepo = InMemoryWhitelistRecordRepository(
            records = mutableMapOf(
                UUID.fromString("00000000-0000-0000-0000-000000000001") to WhitelistRecordData(
                    uniqueId = UUID.fromString("00000000-0000-0000-0000-000000000001"),
                    username = "old",
                    granter = WhitelistOperator.Console,
                    createdAt = Instant.parse("2026-02-06T00:00:00Z"),
                    joinedAsVisitorBefore = false,
                    isMigrated = false,
                    isRevoked = false,
                    revoker = null,
                    revokeReason = null,
                    revokeAt = null,
                )
            )
        )
        val visitorRepo = InMemoryVisitorRecordRepository()
        val core = WhitelistCore(whitelistRepo, visitorRepo, clock)

        val ok = core.grantWhitelist(
            uniqueId = UUID.fromString("00000000-0000-0000-0000-000000000001"),
            username = "new",
            operator = WhitelistOperator.Administrator(UUID.fromString("00000000-0000-0000-0000-0000000000aa")),
        )

        assertFalse(ok)
        assertEquals("old", whitelistRepo.records.getValue(UUID.fromString("00000000-0000-0000-0000-000000000001")).username)
    }

    @Test
    fun `grantWhitelist reactivates existing revoked record and keeps createdAt`() = runBlocking {
        val clock = fixedClock("2026-02-07T00:00:00Z")
        val uid = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val whitelistRepo = InMemoryWhitelistRecordRepository(
            records = mutableMapOf(
                uid to WhitelistRecordData(
                    uniqueId = uid,
                    username = "old",
                    granter = WhitelistOperator.Console,
                    createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                    joinedAsVisitorBefore = false,
                    isMigrated = true,
                    isRevoked = true,
                    revoker = WhitelistOperator.Console,
                    revokeReason = WhitelistRevokeReason.VIOLATION,
                    revokeAt = Instant.parse("2026-02-01T00:00:00Z"),
                )
            )
        )
        val visitorRepo = InMemoryVisitorRecordRepository(hasByUniqueId = setOf(uid))
        val core = WhitelistCore(whitelistRepo, visitorRepo, clock)

        val ok = core.grantWhitelist(uid, "newname", WhitelistOperator.Console)
        assertTrue(ok)

        val updated = whitelistRepo.records.getValue(uid)
        assertEquals("newname", updated.username)
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), updated.createdAt)
        assertTrue(updated.joinedAsVisitorBefore)
        assertFalse(updated.isRevoked)
        assertNull(updated.revoker)
        assertNull(updated.revokeReason)
        assertNull(updated.revokeAt)
        assertTrue(updated.isMigrated)
    }

    @Test
    fun `revokeWhitelist sets revocation fields when active`() = runBlocking {
        val clock = fixedClock("2026-02-07T12:00:00Z")
        val uid = UUID.fromString("00000000-0000-0000-0000-000000000003")
        val whitelistRepo = InMemoryWhitelistRecordRepository(
            records = mutableMapOf(
                uid to WhitelistRecordData(
                    uniqueId = uid,
                    username = "abc",
                    granter = WhitelistOperator.Console,
                    createdAt = Instant.parse("2026-02-01T00:00:00Z"),
                    joinedAsVisitorBefore = false,
                    isMigrated = false,
                    isRevoked = false,
                    revoker = null,
                    revokeReason = null,
                    revokeAt = null,
                )
            )
        )
        val core = WhitelistCore(whitelistRepo, InMemoryVisitorRecordRepository(), clock)

        val ok = core.revokeWhitelist(uid, WhitelistOperator.Console, WhitelistRevokeReason.REQUESTED)
        assertTrue(ok)

        val updated = whitelistRepo.records.getValue(uid)
        assertTrue(updated.isRevoked)
        assertEquals(WhitelistOperator.Console, updated.revoker)
        assertEquals(WhitelistRevokeReason.REQUESTED, updated.revokeReason)
        assertEquals(Instant.parse("2026-02-07T12:00:00Z"), updated.revokeAt)
    }

    @Test
    fun `createVisitorRecord persists and returns created record`() = runBlocking {
        val clock = fixedClock("2026-02-07T00:00:00Z")
        val visitorRepo = InMemoryVisitorRecordRepository()
        val core = WhitelistCore(InMemoryWhitelistRecordRepository(), visitorRepo, clock)

        val uid = UUID.fromString("00000000-0000-0000-0000-000000000004")
        val record = core.createVisitorRecord(
            uniqueId = uid,
            params = VisitorRecordParams(
                ipAddress = InetAddress.getByName("127.0.0.1"),
                virtualHost = InetSocketAddress("localhost", 25565),
                visitedAt = Instant.parse("2026-02-07T00:00:00Z"),
                duration = 5.seconds,
                visitedServers = listOf("lobby"),
            )
        )

        assertNotNull(record)
        assertEquals(uid, record.uniqueId)
        assertEquals(1, visitorRepo.records.size)
        assertEquals(clock.instant(), visitorRepo.records.single().createdAt)
    }

    private fun fixedClock(instant: String): Clock {
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC)
    }

    private class InMemoryWhitelistRecordRepository(
        val records: MutableMap<UUID, WhitelistRecordData> = mutableMapOf(),
    ) : WhitelistRecordRepository {
        override suspend fun findByUniqueId(uniqueId: UUID): WhitelistRecordData? {
            return records[uniqueId]
        }

        override suspend fun findActiveByUniqueId(uniqueId: UUID): WhitelistRecordData? {
            return records[uniqueId]?.takeIf { !it.isRevoked }
        }

        override suspend fun hasActiveByUniqueId(uniqueId: UUID): Boolean {
            return findActiveByUniqueId(uniqueId) != null
        }

        override suspend fun count(): Long {
            return records.size.toLong()
        }

        override suspend fun countActive(): Long {
            return records.values.count { !it.isRevoked }.toLong()
        }

        override suspend fun insertAll(records: List<WhitelistRecordData>) {
            records.forEach { this.records[it.uniqueId] = it }
        }

        override suspend fun saveOrUpdate(record: WhitelistRecordData) {
            records[record.uniqueId] = record
        }
    }

    private class InMemoryVisitorRecordRepository(
        private val hasByUniqueId: Set<UUID> = emptySet(),
        val records: MutableList<VisitorRecordData> = mutableListOf(),
    ) : VisitorRecordRepository {
        override suspend fun hasByUniqueId(uniqueId: UUID): Boolean {
            return uniqueId in hasByUniqueId || records.any { it.uniqueId == uniqueId }
        }

        override suspend fun findByUniqueId(uniqueId: UUID): List<VisitorRecordData> {
            return records.filter { it.uniqueId == uniqueId }
        }

        override suspend fun save(record: VisitorRecordData) {
            records.add(record)
        }

        override suspend fun findByCidr(cidr: String): List<VisitorRecordData> {
            return emptyList()
        }

        override suspend fun findByIpAddress(ipAddress: InetAddress): List<VisitorRecordData> {
            return emptyList()
        }
    }
}
