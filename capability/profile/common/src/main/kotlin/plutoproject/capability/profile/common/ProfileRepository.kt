package plutoproject.capability.profile.common

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import java.util.*

class ProfileRepository(private val collection: MongoCollection<ProfileDocument>) {
    private val replaceOptions = ReplaceOptions().upsert(true)

    suspend fun findByUniqueId(uuid: UUID): ProfileDocument? =
        collection.find(eq("uuid", uuid.toString())).firstOrNull()

    suspend fun findByName(name: String): ProfileDocument? =
        collection.find(eq("lowercasedName", name.lowercase())).firstOrNull()

    suspend fun save(document: ProfileDocument) {
        collection.insertOne(document)
    }

    suspend fun update(document: ProfileDocument) {
        collection.replaceOne(eq("uuid", document.uuid.toString()), document, replaceOptions)
    }
}
