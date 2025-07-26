package plutoproject.feature.paper.exchangeshop.repositories

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.ClientSession
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document
import org.bson.conversions.Bson
import plutoproject.feature.paper.exchangeshop.models.TransactionModel
import plutoproject.framework.common.util.database.upsertReplaceOptions
import java.util.*

class TransactionRepository(private val collection: MongoCollection<TransactionModel>) {
    fun find(filter: Bson = Document(), skip: Int? = null, limit: Int? = null): Flow<TransactionModel> {
        return collection.find(filter)
            .apply { if (skip != null) skip(skip) }
            .apply { if (limit != null) limit(limit) }
    }

    suspend fun find(id: UUID): TransactionModel? {
        return find(Filters.eq("_id", id)).firstOrNull()
    }

    suspend fun count(filter: Bson = Document()): Long {
        return collection.countDocuments(filter)
    }

    suspend fun insert(model: TransactionModel) {
        collection.replaceOne(Filters.eq("_id", model.id), model, upsertReplaceOptions())
    }

    suspend fun insert(clientSession: ClientSession, model: TransactionModel) {
        collection.replaceOne(clientSession, Filters.eq("_id", model.id), model, upsertReplaceOptions())
    }
}
