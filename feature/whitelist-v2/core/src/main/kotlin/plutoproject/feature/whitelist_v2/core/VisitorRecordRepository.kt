package plutoproject.feature.whitelist_v2.core

import java.net.InetAddress
import java.util.UUID

interface VisitorRecordRepository {
    suspend fun hasByUniqueId(uniqueId: UUID): Boolean

    suspend fun findByUniqueId(uniqueId: UUID): List<VisitorRecord>

    suspend fun save(record: VisitorRecord)

    suspend fun findByCidr(cidr: String): List<VisitorRecord>

    suspend fun findByIpAddress(ipAddress: InetAddress): List<VisitorRecord>
}
