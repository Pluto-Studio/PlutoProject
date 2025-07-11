package plutoproject.framework.common.databasepersist

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.ChangeStreamFlow
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document
import org.bson.conversions.Bson
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class ContainerRepository : KoinComponent {
    private val collection by inject<MongoCollection<ContainerModel>>()
    private val replaceOptions = ReplaceOptions().upsert(true)

    fun openChangeStream(pipelines: List<Bson>): ChangeStreamFlow<Document> {
        return collection.withDocumentClass<Document>().watch(pipelines)
    }

    suspend fun findByPlayerId(playerId: UUID, projection: Bson): Document? {
        return collection
            .withDocumentClass<Document>()
            .find(Filters.eq(ContainerModel::playerId.name, playerId))
            .projection(projection)
            .firstOrNull()
    }

    suspend fun updateDocument(playerId: UUID, updates: Bson) {
        collection.updateOne(Filters.eq(ContainerModel::playerId.name, playerId), updates)
    }

    suspend fun save(model: ContainerModel) {
        collection.replaceOne(Filters.eq(ContainerModel::playerId.name, model.playerId), model, replaceOptions)
    }
}
