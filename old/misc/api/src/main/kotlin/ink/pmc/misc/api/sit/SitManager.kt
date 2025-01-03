package ink.pmc.misc.api.sit

import ink.pmc.framework.inject.inlinedGet
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

interface SitManager {

    companion object : SitManager by inlinedGet()

    val sitters: Set<Player>
    val seats: Set<Entity>

    fun sit(player: Player, location: Location)

    fun isSitting(player: Player): Boolean

    fun stand(player: Player)

    fun getSeat(player: Player): Entity?

    fun getSitLocation(player: Player): Location?

    fun getSitterByLocation(location: Location): Player?

    fun getSitterBySeat(seat: Entity): Player?

    fun isSeat(entity: Entity): Boolean

    fun isSitLocation(location: Location): Boolean

    fun standAll()

}