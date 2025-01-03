package ink.pmc.essentials.repositories

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOptions
import ink.pmc.essentials.config.EssentialsConfig
import ink.pmc.essentials.models.BackModel
import ink.pmc.framework.provider.Provider
import ink.pmc.framework.storage.model
import kotlinx.coroutines.flow.firstOrNull
import org.bson.types.ObjectId
import org.bukkit.Location
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BackRepository : KoinComponent {
    private val config by inject<EssentialsConfig>()
    private val options = ReplaceOptions().upsert(true)
    private val db =
        Provider.defaultMongoDatabase.getCollection<BackModel>("essentials_${config.serverName}_backs")

    suspend fun find(player: Player): Location? {
        return findModel(player)?.location?.location
    }

    private suspend fun findModel(player: Player): BackModel? {
        return db.find(eq("owner", player.uniqueId.toString())).firstOrNull()
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
            location = location.model
        )

        if (existed != null) {
            model.recordedAt = System.currentTimeMillis()
            model.location = location.model
        }

        db.replaceOne(eq("owner", player.uniqueId.toString()), model, options)
    }

    suspend fun delete(player: Player) {
        db.deleteOne(eq("owner", player.uniqueId.toString()))
    }
}