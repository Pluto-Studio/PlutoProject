package plutoproject.framework.common.databasepersist

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.changestream.ChangeStreamDocument
import com.mongodb.client.model.changestream.OperationType
import kotlinx.coroutines.*
import org.bson.BsonDocument
import org.bson.Document
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.framework.common.api.connection.MongoConnection
import plutoproject.framework.common.api.databasepersist.adapters.SerializationTypeAdapter
import plutoproject.framework.common.util.coroutine.Loom
import plutoproject.framework.common.util.coroutine.PluginScope
import plutoproject.framework.common.util.data.map.mutableConcurrentMapOf
import plutoproject.framework.common.util.logger
import plutoproject.framework.common.util.serverName
import java.util.*
import java.util.logging.Level

typealias ActionOnChange = (event: ChangeStreamDocument<Document>) -> Unit

val FULL_DOCUMENT_OPERATION_TYPES = listOf(OperationType.INSERT, OperationType.REPLACE)

class DataChangeStream : KoinComponent {
    private var isValid = true
    private val repository by inject<ContainerRepository>()
    private val updateInfoAdapter = SerializationTypeAdapter<UpdateInfo>()
    private val subscribers = mutableConcurrentMapOf<UUID, ActionOnChange>()

    private val match = Aggregates.match(
        Filters.or(
            Filters.and(
                Filters.`in`("operationType", listOf("insert", "replace")),
                Filters.exists("fullDocument.updateInfo"),
                Filters.ne("fullDocument.updateInfo.server", serverName)
            ),
            Filters.and(
                Filters.eq("operationType", "update"),
                Filters.exists("updateDescription.updatedFields.updateInfo"),
                Filters.ne("updateDescription.updatedFields.updateInfo.server", serverName)
            )
        )
    )

    private val changeStreamFlow = repository.openChangeStream(listOf(match))
    private var changeStreamJob = runChangeStreamJob()

    private fun runChangeStreamJob(): Job = PluginScope.launch(Dispatchers.Loom) {
        changeStreamFlow.collect { event ->
            val playerId = extractPlayerId(event)
            runCatching {
                subscribers[playerId]?.invoke(event)
            }.onFailure {
                logger.log(Level.SEVERE, "Error calling subscriber for $playerId", it)
            }
        }
    }.apply {
        invokeOnCompletion {
            if (it is CancellationException) return@invokeOnCompletion
            logger.log(Level.SEVERE, "Error occurred while listening to change stream", it)
            changeStreamJob = runChangeStreamJob()
        }
    }

    private fun extractPlayerId(event: ChangeStreamDocument<Document>): UUID {
        val updateInfo = when (event.operationType) {
            in FULL_DOCUMENT_OPERATION_TYPES -> (event.fullDocument?.getValue(ContainerModel::updateInfo.name) as Document)
                .toBsonDocument(BsonDocument::class.java, MongoConnection.client.codecRegistry)

            OperationType.UPDATE -> event.updateDescription?.updatedFields?.getValue(ContainerModel::updateInfo.name) as BsonDocument
            else -> error("Unexpected")
        }
        return updateInfoAdapter.fromBson(updateInfo).playerId
    }

    fun subscribe(playerId: UUID, action: ActionOnChange) {
        check(isValid) { "DataChangeStream instance already closed." }
        subscribers[playerId] = action
    }

    fun unsubscribe(playerId: UUID) {
        check(isValid) { "DataChangeStream instance already closed." }
        subscribers.remove(playerId)
    }

    fun close() = runBlocking {
        isValid = false
        changeStreamJob.cancelAndJoin()
    }
}
