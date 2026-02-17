package plutoproject.feature.whitelist_v2.core.usecase

import plutoproject.feature.whitelist_v2.core.VisitorRecordData
import plutoproject.feature.whitelist_v2.core.VisitorRecordRepository
import java.net.InetAddress

class LookupVisitorRecordsByIpUseCase(
    private val visitorRecords: VisitorRecordRepository,
) {
    suspend fun execute(ipAddress: InetAddress): List<VisitorRecordData> {
        return visitorRecords.findByIpAddress(ipAddress)
    }
}
