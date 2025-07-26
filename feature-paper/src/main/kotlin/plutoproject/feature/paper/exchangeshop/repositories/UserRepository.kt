package plutoproject.feature.paper.exchangeshop.repositories

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import com.mongodb.kotlin.client.coroutine.ClientSession
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import org.bson.conversions.Bson
import plutoproject.feature.paper.exchangeshop.models.UserModel
import plutoproject.framework.common.util.database.upsertReplaceOptions
import java.util.*

class UserRepository(private val collection: MongoCollection<UserModel>) {
    suspend fun has(uniqueId: UUID): Boolean {
        return collection.find<UserModel>(Filters.eq("_id", uniqueId))
            .projection(Projections.include("_id"))
            .limit(1)
            .firstOrNull() != null
    }

    suspend fun find(uniqueId: UUID): UserModel? {
        return collection.find(Filters.eq("_id", uniqueId)).firstOrNull()
    }

    suspend fun update(uniqueId: UUID, update: Bson) {
        collection.updateOne(Filters.eq("_id", uniqueId), update)
    }

    suspend fun update(clientSession: ClientSession, uniqueId: UUID, update: Bson) {
        collection.updateOne(clientSession, Filters.eq("_id", uniqueId), update)
    }

    suspend fun insert(model: UserModel) {
        collection.replaceOne(Filters.eq("_id", model.uniqueId), model, upsertReplaceOptions())
    }
}
