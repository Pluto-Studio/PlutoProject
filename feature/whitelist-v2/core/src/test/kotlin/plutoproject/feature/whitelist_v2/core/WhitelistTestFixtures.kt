package plutoproject.feature.whitelist_v2.core

import java.net.InetAddress
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

internal fun fixedClock(instant: String): Clock {
    return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC)
}

internal fun dummyUuid(value: Long): UUID {
    return UUID(0L, value)
}

internal class InMemoryWhitelistRecordRepository(
    val records: MutableMap<UUID, WhitelistRecord> = mutableMapOf(),
) : WhitelistRecordRepository {
    override suspend fun findByUniqueId(uniqueId: UUID): WhitelistRecord? {
        return records[uniqueId]
    }

    override suspend fun findActiveByUniqueId(uniqueId: UUID): WhitelistRecord? {
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

    override suspend fun insertAll(records: List<WhitelistRecord>) {
        records.forEach { this.records[it.uniqueId] = it }
    }

    override suspend fun saveOrUpdate(record: WhitelistRecord) {
        records[record.uniqueId] = record
    }
}

internal class InMemoryVisitorRecordRepository(
    private val hasByUniqueId: Set<UUID> = emptySet(),
    val records: MutableList<VisitorRecord> = mutableListOf(),
) : VisitorRecordRepository {
    override suspend fun hasByUniqueId(uniqueId: UUID): Boolean {
        return uniqueId in hasByUniqueId || records.any { it.uniqueId == uniqueId }
    }

    override suspend fun findByUniqueId(uniqueId: UUID): List<VisitorRecord> {
        return records.filter { it.uniqueId == uniqueId }
    }

    override suspend fun save(record: VisitorRecord) {
        records.add(record)
    }

    override suspend fun findByCidr(cidr: String): List<VisitorRecord> {
        return emptyList()
    }

    override suspend fun findByIpAddress(ipAddress: InetAddress): List<VisitorRecord> {
        return emptyList()
    }
}
