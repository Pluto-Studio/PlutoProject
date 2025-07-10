package plutoproject.framework.common.databasepersist

import com.mongodb.kotlin.client.coroutine.MongoCollection
import plutoproject.framework.common.api.databasepersist.PersistContainer
import java.util.*

class DatabasePersistImpl(private val collection: MongoCollection<ContainerModel>) : InternalDatabasePersist {
    override fun getContainer(playerId: UUID): PersistContainer {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}
