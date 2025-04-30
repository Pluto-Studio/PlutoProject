package plutoproject.framework.common.profile

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import java.util.*

class ProfileRepository(private val collection: MongoCollection<ProfileModel>) {
    private val replaceOptions = ReplaceOptions().upsert(true)

    suspend fun findByUniqueId(uuid: UUID): ProfileModel? {
        return collection.find(eq("uuid", uuid.toString())).firstOrNull()
    }

    suspend fun findByName(name: String): ProfileModel? {
        return collection.find(eq("lowercasedName", name.lowercase())).firstOrNull()
    }

    suspend fun save(model: ProfileModel) {
        collection.insertOne(model)
    }

    suspend fun update(model: ProfileModel) {
        collection.replaceOne(eq("uuid", model.uuid.toString()), model, replaceOptions)
    }
}
