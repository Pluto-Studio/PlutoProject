package plutoproject.feature.whitelist_v2.core.usecase

import plutoproject.feature.whitelist_v2.core.VisitorRecordData
import plutoproject.feature.whitelist_v2.core.VisitorRecordParams
import plutoproject.feature.whitelist_v2.core.VisitorRecordRepository
import java.time.Clock
import java.util.*

class CreateVisitorRecordUseCase(
    private val visitorRecords: VisitorRecordRepository,
    private val clock: Clock,
) {
    suspend fun execute(uniqueId: UUID, params: VisitorRecordParams): VisitorRecordData {
        val record = VisitorRecordData(
            uniqueId = uniqueId,
            ipAddress = params.ipAddress,
            virtualHost = params.virtualHost,
            visitedAt = params.visitedAt,
            createdAt = clock.instant(),
            duration = params.duration,
            visitedServers = params.visitedServers,
        )
        visitorRecords.save(record)
        return record
    }
}
