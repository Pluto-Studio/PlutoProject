package plutoproject.feature.common.serverSelector

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import java.util.*

class UserRepository(private val collection: MongoCollection<UserModel>) {
    private val replaceOptions = ReplaceOptions().upsert(true)

    suspend fun findOrCreate(uuid: UUID): UserModel {
        return find(uuid) ?: create(uuid)
    }

    suspend fun find(uuid: UUID): UserModel? {
        return collection.find(eq("uuid", uuid)).firstOrNull()
    }

    suspend fun create(uuid: UUID): UserModel {
        val data = UserModel(
            uuid = uuid,
            hasJoinedBefore = false,
            previouslyJoinedServer = null
        )
        saveOrUpdate(data)
        return data
    }

    suspend fun saveOrUpdate(data: UserModel) {
        collection.replaceOne(eq("uuid", data.uuid), data, replaceOptions)
    }
}
