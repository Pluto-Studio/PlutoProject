package plutoproject.feature.whitelist_v2.application

import java.net.InetAddress
import java.util.UUID

interface VisitorRecordRepository {
    suspend fun hasByUniqueId(uniqueId: UUID): Boolean

    suspend fun findByUniqueId(uniqueId: UUID): List<VisitorRecordData>

    suspend fun save(record: VisitorRecordData)

    suspend fun findByCidr(cidr: String): List<VisitorRecordData>

    suspend fun findByIpAddress(ipAddress: InetAddress): List<VisitorRecordData>
}
