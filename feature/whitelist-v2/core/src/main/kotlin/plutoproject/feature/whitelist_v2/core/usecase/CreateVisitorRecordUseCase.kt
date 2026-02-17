package plutoproject.feature.whitelist_v2.core.usecase

import plutoproject.feature.whitelist_v2.core.VisitorRecord
import plutoproject.feature.whitelist_v2.core.VisitorRecordParams
import plutoproject.feature.whitelist_v2.core.VisitorRecordRepository
import java.time.Clock
import java.util.*

class CreateVisitorRecordUseCase(
    private val visitorRecords: VisitorRecordRepository,
    private val clock: Clock,
) {
    suspend fun execute(uniqueId: UUID, params: VisitorRecordParams): VisitorRecord {
        val record = VisitorRecord(
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
