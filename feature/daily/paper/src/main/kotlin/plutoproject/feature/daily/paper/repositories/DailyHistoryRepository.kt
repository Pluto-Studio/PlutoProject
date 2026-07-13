package plutoproject.feature.daily.paper.repositories

import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toCollection
import plutoproject.feature.daily.paper.models.DailyHistoryModel
import plutoproject.foundation.common.time.LocalZoneId
import plutoproject.foundation.common.time.atEndOfDay
import plutoproject.foundation.common.time.toOffset
import java.time.Instant
import java.time.LocalDate
import java.util.*

class DailyHistoryRepository(private val collection: MongoCollection<DailyHistoryModel>) {
    private val upsert = ReplaceOptions().upsert(true)

    suspend fun findById(id: UUID): DailyHistoryModel? {
        return collection.find(eq("_id", id.toString())).firstOrNull()
    }

    suspend fun findByOwner(owner: UUID): List<DailyHistoryModel> {
        return collection.find(eq("owner", owner.toString())).toCollection(mutableListOf())
    }

    suspend fun findByTime(owner: UUID, start: Instant, end: Instant): List<DailyHistoryModel> {
        return findByTime(owner, start.toEpochMilli(), end.toEpochMilli())
    }

    suspend fun findByTime(owner: UUID, start: Long, end: Long): List<DailyHistoryModel> {
        return collection.find(
            and(
                eq("owner", owner.toString()),
                gte("createdAt", start),
                lte("createdAt", end)
            )
        ).toCollection(mutableListOf())
    }

    suspend fun findByTime(owner: UUID, date: LocalDate): DailyHistoryModel? {
        val start = date.atStartOfDay().toInstant(LocalZoneId.toOffset())
        val end = date.atEndOfDay().toInstant(LocalZoneId.toOffset())
        return findByTime(owner, start, end).firstOrNull()
    }

    suspend fun deleteById(id: UUID) {
        collection.deleteOne(eq("_id", id.toString()))
    }

    suspend fun deleteByTime(owner: UUID, start: Instant, end: Instant) {
        return deleteByTime(owner, start.toEpochMilli(), end.toEpochMilli())
    }

    suspend fun deleteByTime(owner: UUID, start: Long, end: Long) {
        collection.deleteMany(
            and(
                eq("owner", owner.toString()),
                gte("createdAt", start),
                lte("createdAt", end)
            )
        )
    }

    suspend fun saveOrUpdate(model: DailyHistoryModel) {
        collection.replaceOne(eq("_id", model.id), model, upsert)
    }
}
