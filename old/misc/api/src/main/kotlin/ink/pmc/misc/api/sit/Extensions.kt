package ink.pmc.misc.api.sit

import org.bukkit.Location
import org.bukkit.Server
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

fun Player.sit(location: Location) {
    SitManager.sit(this, location)
}

val Player.seat: Entity?
    get() = SitManager.getSeat(this)
val Player.sitLocation: Location?
    get() = SitManager.getSitLocation(this)
var Player.isSitting: Boolean
    get() = SitManager.isSitting(this)
    set(value) {
        if (!value) {
            this.stand()
        }
    }

fun Player.stand() {
    SitManager.stand(this)
}

val Location.sitter: Player?
    get() = SitManager.getSitterByLocation(this)

val Entity.sitter: Player?
    get() = SitManager.getSitterBySeat(this)

val Entity.isSeat: Boolean
    get() = SitManager.isSeat(this)

val Location.isSitLocation: Boolean
    get() = SitManager.isSitLocation(this)

fun Server.standAll() {
    SitManager.standAll()
}