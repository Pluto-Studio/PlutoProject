package plutoproject.framework.common.options.repositories

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import plutoproject.framework.common.options.models.PlayerOptionsModel
import java.util.*

class OptionsContainerRepository(private val collection: MongoCollection<PlayerOptionsModel>) {
    private val upsert = ReplaceOptions().upsert(true)

    suspend fun findById(uuid: UUID): PlayerOptionsModel? {
        return collection.find(eq("_id", uuid.toString())).firstOrNull()
    }

    suspend fun deleteById(uuid: UUID) {
        collection.deleteOne(eq("_id", uuid.toString()))
    }

    suspend fun saveOrUpdate(model: PlayerOptionsModel) {
        collection.replaceOne(eq("_id", model.id), model, upsert)
    }
}
