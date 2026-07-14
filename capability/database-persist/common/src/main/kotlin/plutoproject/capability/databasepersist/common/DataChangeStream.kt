package plutoproject.capability.databasepersist.common

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.changestream.ChangeStreamDocument
import com.mongodb.client.model.changestream.OperationType
import kotlinx.coroutines.*
import org.bson.BsonDocument
import org.bson.Document
import plutoproject.capability.databasepersist.api.adapters.SerializationTypeAdapter
import plutoproject.capability.mongo.api.MongoConnection
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger

typealias ActionOnChange = (event: ChangeStreamDocument<Document>) -> Unit

val FULL_DOCUMENT_OPERATION_TYPES = listOf(OperationType.INSERT, OperationType.REPLACE)

class DataChangeStream(
    private val scope: CoroutineScope,
    private val repository: ContainerRepository,
    private val mongoConnection: MongoConnection,
    private val logger: Logger,
    private val serverIdentifier: String,
) {
    private val stateLock = Any()
    private var isValid = true
    private val updateInfoAdapter =
        SerializationTypeAdapter<UpdateInfo>()
    private val subscribers = ConcurrentHashMap<UUID, ActionOnChange>()

    private val match = Aggregates.match(
        Filters.or(
            Filters.and(
                Filters.`in`("operationType", listOf("insert", "replace")),
                Filters.exists("fullDocument.updateInfo"),
                Filters.ne("fullDocument.updateInfo.server", serverIdentifier),
            ),
            Filters.and(
                Filters.eq("operationType", "update"),
                Filters.exists("updateDescription.updatedFields.updateInfo"),
                Filters.ne("updateDescription.updatedFields.updateInfo.server", serverIdentifier),
            ),
        ),
    )

    private val changeStreamFlow = repository.openChangeStream(listOf(match))
    private var changeStreamJob = runChangeStreamJob()

    private fun runChangeStreamJob(): Job = scope.launch(Dispatchers.IO) {
        changeStreamFlow.collect { event ->
            val playerId = extractPlayerId(event)
            runCatching {
                subscribers[playerId]?.invoke(event)
            }.onFailure {
                logger.log(Level.SEVERE, "Error calling subscriber for $playerId", it)
            }
        }
    }.also { job ->
        job.invokeOnCompletion { cause ->
            synchronized(stateLock) {
                if (cause is CancellationException || !isValid) return@synchronized
                logger.log(Level.SEVERE, "Error occurred while listening to change stream", cause)
                changeStreamJob = runChangeStreamJob()
            }
        }
    }

    private fun extractPlayerId(event: ChangeStreamDocument<Document>): UUID {
        val operationType = event.operationType ?: return error("Missing operation type")
        val updateInfo = when (operationType) {
            in FULL_DOCUMENT_OPERATION_TYPES -> (event.fullDocument
                ?.getValue(ContainerModel::updateInfo.name) as Document)
                .toBsonDocument(BsonDocument::class.java, mongoConnection.client.codecRegistry)

            OperationType.UPDATE -> event.updateDescription
                ?.updatedFields
                ?.getValue(ContainerModel::updateInfo.name) as BsonDocument

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

    suspend fun close() {
        val job = synchronized(stateLock) {
            if (!isValid) {
                null
            } else {
                isValid = false
                subscribers.clear()
                changeStreamJob
            }
        } ?: return
        job.cancelAndJoin()
    }
}
