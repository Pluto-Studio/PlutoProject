package plutoproject.framework.common.databasepersist

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.changestream.ChangeStreamDocument
import com.mongodb.client.model.changestream.OperationType.*
import org.bson.BsonBinary
import org.bson.BsonDocument
import org.bson.Document
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.framework.common.util.coroutine.runAsync
import plutoproject.framework.common.util.data.map.mutableConcurrentMapOf
import plutoproject.framework.common.util.serverName
import java.util.*

typealias ActionOnChange = (event: ChangeStreamDocument<Document>) -> Unit

class DataChangeStream : KoinComponent {
    private var isValid = true
    private val repository by inject<ContainerRepository>()
    private val subscribers = mutableConcurrentMapOf<UUID, ActionOnChange>()

    private val match = Aggregates.match(
        Filters.or(
            Filters.and(
                Filters.`in`("operationType", listOf("insert", "replace")),
                Filters.exists("fullDocument.updatedByServer"),
                Filters.ne("fullDocument.updatedByServer", serverName)
            ),
            Filters.and(
                Filters.eq("operationType", "update"),
                Filters.exists("updateDescription.updatedFields.updatedByServer"),
                Filters.ne("updateDescription.updatedFields.updatedByServer", serverName)
            )
        )
    )

    private val changeStreamFlow = repository.openChangeStream(listOf())
    private val changeStreamJob = runAsync {
        println("Change Stream Job Stared")
        changeStreamFlow.collect { event ->
            println("A document changed: $event")
            val playerId = when (event.operationType) {
                INSERT -> extractPlayerId(event.fullDocument!!.toBsonDocument())
                UPDATE -> extractPlayerId(event.fullDocument!!.toBsonDocument())
                REPLACE -> extractPlayerId(event.updateDescription!!.updatedFields!!)
                else -> error("Unexpected")
            }
            subscribers[playerId]?.invoke(event)
        }
    }

    private fun extractPlayerId(document: BsonDocument): UUID {
        val playerIdBinary = document.getValue("playerId") as BsonBinary
        return playerIdBinary.asUuid()
    }

    fun subscribe(playerId: UUID, action: ActionOnChange) {
        check(isValid) { "DataChangeStream instance already closed." }
        subscribers[playerId] = action
    }

    fun unsubscribe(playerId: UUID) {
        check(isValid) { "DataChangeStream instance already closed." }
        subscribers.remove(playerId)
    }

    fun close() {
        isValid = false
        changeStreamJob.cancel()
    }
}
