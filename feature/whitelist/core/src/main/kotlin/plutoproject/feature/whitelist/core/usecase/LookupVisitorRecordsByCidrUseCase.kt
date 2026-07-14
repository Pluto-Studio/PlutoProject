package plutoproject.feature.whitelist.core.usecase

import plutoproject.feature.whitelist.core.VisitorRecord
import plutoproject.feature.whitelist.core.VisitorRecordRepository

class LookupVisitorRecordsByCidrUseCase(
    private val visitorRecords: VisitorRecordRepository,
) {
    suspend fun execute(cidr: String): List<VisitorRecord> {
        return visitorRecords.findByCidr(cidr)
    }
}
