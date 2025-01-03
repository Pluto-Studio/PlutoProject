package ink.pmc.whitelist.repositories

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.velocitypowered.api.proxy.Player
import ink.pmc.whitelist.models.WhitelistModel
import kotlinx.coroutines.flow.firstOrNull
import java.util.*

@Suppress("UNUSED")
class WhitelistRepository(private val collection: MongoCollection<WhitelistModel>) {
    private val upsert = ReplaceOptions().upsert(true)

    suspend fun findById(id: UUID): WhitelistModel? {
        return collection.find(eq("_id", id.toString())).firstOrNull()
    }

    suspend fun findByName(name: String): WhitelistModel? {
        return collection.find(eq("name", name.lowercase())).firstOrNull()
    }

    suspend fun findByPlayer(player: Player): WhitelistModel? {
        return findById(player.uniqueId)
    }

    suspend fun hasById(id: UUID): Boolean {
        return findById(id) != null
    }

    suspend fun hasByPlayer(player: Player): Boolean {
        return hasById(player.uniqueId)
    }

    suspend fun hasByName(name: String): Boolean {
        return findByName(name) != null
    }

    suspend fun deleteById(id: UUID) {
        collection.deleteOne(eq("_id", id.toString()))
    }

    suspend fun deleteByPlayer(player: Player) {
        deleteById(player.uniqueId)
    }

    suspend fun deleteByName(name: String) {
        collection.deleteOne(eq("name", name.lowercase()))
    }

    suspend fun count(): Long {
        return collection.countDocuments()
    }

    suspend fun saveOrUpdate(model: WhitelistModel) {
        collection.replaceOne(eq("_id", model.id), model, upsert)
    }
}