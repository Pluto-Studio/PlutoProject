package plutoproject.feature.whitelist_v2.infra.mongo

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.gte
import com.mongodb.client.model.Filters.lte
import com.mongodb.client.model.Indexes
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.toList
import plutoproject.feature.whitelist_v2.core.VisitorRecord
import plutoproject.feature.whitelist_v2.core.VisitorRecordRepository
import plutoproject.feature.whitelist_v2.infra.mongo.model.VisitorRecordDocument
import java.net.Inet6Address
import java.net.InetAddress
import java.util.UUID

class MongoVisitorRecordRepository(
    private val collection: MongoCollection<VisitorRecordDocument>,
) : VisitorRecordRepository {
    suspend fun ensureIndexes() {
        collection.createIndex(
            Indexes.compoundIndex(
                Indexes.ascending("ipAddress.ipVersion"),
                Indexes.ascending("ipAddress.ipHigh"),
                Indexes.ascending("ipAddress.ipLow"),
            )
        )
    }

    override suspend fun hasByUniqueId(uniqueId: UUID): Boolean {
        return collection.find(eq("uniqueId", uniqueId)).limit(1).toList().isNotEmpty()
    }

    override suspend fun findByUniqueId(uniqueId: UUID): List<VisitorRecord> {
        return collection.find(eq("uniqueId", uniqueId))
            .sort(Indexes.descending("createdAt"))
            .toList()
            .map { it.toDomain() }
    }

    override suspend fun save(record: VisitorRecord) {
        collection.insertOne(record.toDocument())
    }

    override suspend fun findByCidr(cidr: String): List<VisitorRecord> {
        return when (val range = cidr.toIpRange()) {
            is IpRange.Ipv4 -> findByIpv4Range(range)
            is IpRange.Ipv6 -> findByIpv6Range(range)
        }
    }

    override suspend fun findByIpAddress(ipAddress: InetAddress): List<VisitorRecord> {
        val (high, low) = ipAddress.toLongs()
        val version = if (ipAddress is Inet6Address) 6 else 4

        return collection.find(
            and(
                eq("ipAddress.ipVersion", version),
                eq("ipAddress.ipHigh", high),
                eq("ipAddress.ipLow", low),
            )
        ).toList().map { it.toDomain() }
    }

    private suspend fun findByIpv4Range(range: IpRange.Ipv4): List<VisitorRecord> {
        return collection.find(
            and(
                eq("ipAddress.ipVersion", 4),
                gte("ipAddress.ipLow", range.start),
                lte("ipAddress.ipLow", range.end),
            )
        ).toList().map { it.toDomain() }
    }

    private suspend fun findByIpv6Range(range: IpRange.Ipv6): List<VisitorRecord> {
        val query = if (range.startHigh == range.endHigh) {
            and(
                eq("ipAddress.ipVersion", 6),
                eq("ipAddress.ipHigh", range.startHigh),
                gte("ipAddress.ipLow", range.startLow),
                lte("ipAddress.ipLow", range.endLow),
            )
        } else {
            and(
                eq("ipAddress.ipVersion", 6),
                gte("ipAddress.ipHigh", range.startHigh),
                lte("ipAddress.ipHigh", range.endHigh),
            )
        }

        return collection.find(query).toList().map { it.toDomain() }
    }
}
