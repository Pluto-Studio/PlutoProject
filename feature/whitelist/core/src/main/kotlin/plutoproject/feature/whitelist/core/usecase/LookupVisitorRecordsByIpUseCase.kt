package plutoproject.feature.whitelist.core.usecase

import plutoproject.feature.whitelist.core.VisitorRecord
import plutoproject.feature.whitelist.core.VisitorRecordRepository
import java.net.InetAddress

class LookupVisitorRecordsByIpUseCase(
    private val visitorRecords: VisitorRecordRepository,
) {
    suspend fun execute(ipAddress: InetAddress): List<VisitorRecord> {
        return visitorRecords.findByIpAddress(ipAddress)
    }
}
