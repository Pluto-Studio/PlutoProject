package plutoproject.feature.back.paper

import plutoproject.foundation.paper.world.toModel

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import org.bson.types.ObjectId
import org.bukkit.Location
import org.bukkit.entity.Player

class BackRepository(private val collection: MongoCollection<BackModel>) {
    private val options = ReplaceOptions().upsert(true)

    suspend fun find(player: Player): Location? {
        return findModel(player)?.location?.toLocation()
    }

    private suspend fun findModel(player: Player): BackModel? {
        return collection.find(eq("owner", player.uniqueId.toString())).firstOrNull()
    }

    suspend fun has(player: Player): Boolean {
        return find(player) != null
    }

    suspend fun save(player: Player, location: Location) {
        val existed = findModel(player)
        val model = existed ?: BackModel(
            objectId = ObjectId(),
            owner = player.uniqueId,
            recordedAt = System.currentTimeMillis(),
            location = location.toModel()
        )

        if (existed != null) {
            model.recordedAt = System.currentTimeMillis()
            model.location = location.toModel()
        }

        collection.replaceOne(eq("owner", player.uniqueId.toString()), model, options)
    }

    suspend fun delete(player: Player) {
        collection.deleteOne(eq("owner", player.uniqueId.toString()))
    }
}
