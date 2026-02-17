package plutoproject.feature.whitelist_v2.core.usecase

import plutoproject.feature.whitelist_v2.core.VisitorRecord
import plutoproject.feature.whitelist_v2.core.VisitorRecordRepository
import java.net.InetAddress

class LookupVisitorRecordsByIpUseCase(
    private val visitorRecords: VisitorRecordRepository,
) {
    suspend fun execute(ipAddress: InetAddress): List<VisitorRecord> {
        return visitorRecords.findByIpAddress(ipAddress)
    }
}
