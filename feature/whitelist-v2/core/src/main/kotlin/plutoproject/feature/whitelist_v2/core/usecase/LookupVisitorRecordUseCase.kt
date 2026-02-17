package plutoproject.feature.whitelist_v2.core.usecase

import plutoproject.feature.whitelist_v2.core.VisitorRecordData
import plutoproject.feature.whitelist_v2.core.VisitorRecordRepository
import java.util.UUID

class LookupVisitorRecordUseCase(
    private val visitorRecords: VisitorRecordRepository,
) {
    suspend fun execute(uniqueId: UUID): List<VisitorRecordData> {
        return visitorRecords.findByUniqueId(uniqueId)
    }
}
