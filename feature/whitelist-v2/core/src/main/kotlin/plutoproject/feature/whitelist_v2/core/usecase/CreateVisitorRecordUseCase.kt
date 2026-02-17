package plutoproject.feature.whitelist_v2.core.usecase

import plutoproject.feature.whitelist_v2.api.VisitorRecord
import plutoproject.feature.whitelist_v2.api.VisitorRecordParams
import plutoproject.feature.whitelist_v2.core.VisitorRecordData
import plutoproject.feature.whitelist_v2.core.VisitorRecordRepository
import java.time.Clock
import java.util.UUID

class CreateVisitorRecordUseCase(
    private val visitorRecords: VisitorRecordRepository,
    private val clock: Clock,
) {
    suspend fun execute(uniqueId: UUID, params: VisitorRecordParams): VisitorRecord {
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
