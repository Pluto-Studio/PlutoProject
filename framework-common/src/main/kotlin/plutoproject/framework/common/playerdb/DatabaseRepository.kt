package plutoproject.framework.common.playerdb

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import java.util.*

class DatabaseRepository(private val collection: MongoCollection<DatabaseModel>) {
    private val upsert = ReplaceOptions().upsert(true)

    suspend fun findById(id: UUID): DatabaseModel? {
        return collection.find(eq("_id", id.toString())).firstOrNull()
    }

    suspend fun hasById(id: UUID): Boolean {
        return findById(id) != null
    }

    suspend fun deleteById(id: UUID) {
        collection.deleteOne(eq("_id", id.toString()))
    }

    suspend fun saveOrUpdate(model: DatabaseModel) {
        collection.replaceOne(eq("_id", model.id), model, upsert)
    }
}
