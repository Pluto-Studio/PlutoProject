package ink.pmc.daily.repositories

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoCollection
import ink.pmc.daily.models.DailyUserModel
import kotlinx.coroutines.flow.firstOrNull
import java.util.*

@Suppress("UNUSED")
class DailyUserRepository(private val collection: MongoCollection<DailyUserModel>) {

    private val upsert = ReplaceOptions().upsert(true)

    suspend fun findById(id: UUID): DailyUserModel? {
        return collection.find(eq("_id", id.toString())).firstOrNull()
    }

    suspend fun deleteById(id: UUID) {
        collection.deleteOne(eq("_id", id.toString()))
    }

    suspend fun saveOrUpdate(model: DailyUserModel) {
        collection.replaceOne(eq("_id", model.id), model, upsert)
    }

}