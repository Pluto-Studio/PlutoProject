package plutoproject.feature.whitelist_v2.core.usecase

import plutoproject.feature.whitelist_v2.core.VisitorRecordData
import plutoproject.feature.whitelist_v2.core.VisitorRecordRepository

class LookupVisitorRecordsByCidrUseCase(
    private val visitorRecords: VisitorRecordRepository,
) {
    suspend fun execute(cidr: String): List<VisitorRecordData> {
        return visitorRecords.findByCidr(cidr)
    }
}
