package plutoproject.feature.common.whitelist_v2.repository

import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Indexes.descending
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import plutoproject.feature.common.whitelist_v2.IpRange
import plutoproject.feature.common.whitelist_v2.model.VisitorRecordModel
import plutoproject.feature.common.whitelist_v2.toIpRange
import plutoproject.feature.common.whitelist_v2.toLongs
import java.net.Inet6Address
import java.net.InetAddress
import java.util.*

class VisitorRecordRepository(private val collection: MongoCollection<VisitorRecordModel>) {
    suspend fun findByUniqueId(uniqueId: UUID): List<VisitorRecordModel> {
        return collection.find(eq("uniqueId", uniqueId))
            .sort(descending("createdAt"))
            .toList()
    }

    suspend fun findLatestByUniqueId(uniqueId: UUID): VisitorRecordModel? {
        return collection.find(eq("uniqueId", uniqueId))
            .sort(descending("createdAt"))
            .firstOrNull()
    }

    suspend fun hasByUniqueId(uniqueId: UUID): Boolean {
        return collection.find(eq("uniqueId", uniqueId)).firstOrNull() != null
    }

    suspend fun findByCidr(cidr: String): List<VisitorRecordModel> {
        return when (val range = cidr.toIpRange()) {
            is IpRange.Ipv4 -> findByIpv4Range(range)
            is IpRange.Ipv6 -> findByIpv6Range(range)
        }
    }

    suspend fun findByIpAddress(address: InetAddress): List<VisitorRecordModel> {
        val (high, low) = address.toLongs()
        val version = if (address is Inet6Address) 6 else 4

        return collection.find(
            and(
                eq("ipAddress.ipVersion", version),
                eq("ipAddress.ipHigh", high),
                eq("ipAddress.ipLow", low)
            )
        ).toList()
    }

    private suspend fun findByIpv4Range(range: IpRange.Ipv4): List<VisitorRecordModel> {
        return collection.find(
            and(
                eq("ipAddress.ipVersion", 4),
                gte("ipAddress.ipLow", range.start),
                lte("ipAddress.ipLow", range.end)
            )
        ).toList()
    }

    private suspend fun findByIpv6Range(range: IpRange.Ipv6): List<VisitorRecordModel> {
        return if (range.startHigh == range.endHigh) {
            // 网段在同一高 64 位内
            collection.find(
                and(
                    eq("ipAddress.ipVersion", 6),
                    eq("ipAddress.ipHigh", range.startHigh),
                    gte("ipAddress.ipLow", range.startLow),
                    lte("ipAddress.ipLow", range.endLow)
                )
            ).toList()
        } else {
            // 跨越多个高 64 位范围（如 /32）
            collection.find(
                and(
                    eq("ipAddress.ipVersion", 6),
                    gte("ipAddress.ipHigh", range.startHigh),
                    lte("ipAddress.ipHigh", range.endHigh)
                )
            ).toList()
        }
    }

    suspend fun save(model: VisitorRecordModel) {
        collection.insertOne(model)
    }

    suspend fun saveAll(models: Collection<VisitorRecordModel>) {
        if (models.isNotEmpty()) {
            collection.insertMany(models.toList())
        }
    }

    suspend fun deleteByUniqueId(uniqueId: UUID) {
        collection.deleteMany(eq("uniqueId", uniqueId))
    }
}
