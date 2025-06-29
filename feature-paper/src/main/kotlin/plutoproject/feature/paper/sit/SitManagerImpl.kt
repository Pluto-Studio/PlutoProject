package plutoproject.feature.paper.sit

import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.block.data.type.Campfire
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import plutoproject.feature.paper.api.sit.SitManager
import plutoproject.feature.paper.api.sit.isSitting
import plutoproject.feature.paper.api.sit.sitter
import plutoproject.feature.paper.api.sit.stand
import plutoproject.framework.paper.util.server
import plutoproject.framework.paper.util.world.location.viewAligned
import java.util.*

class SitManagerImpl : SitManager {
    private val playerToSitLocationMap = mutableMapOf<UUID, Location>()
    private val playerToSeatMap = mutableMapOf<UUID, UUID>()
    override val sitters: Set<Player>
        get() = playerToSitLocationMap.keys.map { Bukkit.getPlayer(it)!! }.toSet()
    override val seats: Set<Entity>
        get() = playerToSeatMap.values.map { Bukkit.getEntity(it)!! }.toSet()

    override fun sit(player: Player, location: Location) {
        if (player.isSitting) {
            player.stand()
        }

        var sitLoc = location.viewAligned()

        if (!checkLocation(location)) {
            val tryFind = findLegalLocation(sitLoc)

            if (tryFind == null) {
                player.sendMessage(SIT_FAILED_ILLEGAL_LOCATION)
                return
            }

            sitLoc = tryFind
        }

        if (sitLoc.sitter != null) {
            player.showTitle(SIT_FAILED_LOCATION_OCCUPIED_BY_OTHERS)
            player.playSound(LOCATION_OCCUPIED_BY_OTHER_SOUND)
            return
        }

        if (sitLoc.block.blockData is Campfire && !isExtinctCampfire(sitLoc.block)) {
            player.sendMessage(SIT_FAILED_ILLEGAL_LOCATION)
            return
        }

        val armorStand = createArmorStand(sitLoc)
        markAsSeat(armorStand, player)
        armorStand.addPassenger(player)
        player.playSitSound()

        playerToSitLocationMap[player.uniqueId] = sitLoc
        playerToSeatMap[player.uniqueId] = armorStand.uniqueId
        markDelay(player)
    }

    private fun Player.playSitSound() {
        val leggings = inventory.leggings
        val sound = if (leggings == null) {
            Sound.ITEM_ARMOR_EQUIP_GENERIC
        } else when (leggings.type) {
            Material.LEATHER_LEGGINGS -> Sound.ITEM_ARMOR_EQUIP_LEATHER
            Material.CHAINMAIL_LEGGINGS -> Sound.ITEM_ARMOR_EQUIP_CHAIN
            Material.IRON_LEGGINGS -> Sound.ITEM_ARMOR_EQUIP_IRON
            Material.GOLDEN_LEGGINGS -> Sound.ITEM_ARMOR_EQUIP_GOLD
            Material.DIAMOND_LEGGINGS -> Sound.ITEM_ARMOR_EQUIP_DIAMOND
            Material.NETHERITE_LEGGINGS -> Sound.ITEM_ARMOR_EQUIP_NETHERITE
            else -> Sound.ITEM_ARMOR_EQUIP_GENERIC
        }
        world.playSound(location, sound, SoundCategory.BLOCKS, 1f, 1f)
    }

    override fun isSitting(player: Player): Boolean {
        return playerToSitLocationMap.containsKey(player.uniqueId)
    }

    override fun stand(player: Player) {
        if (!isSitting(player)) {
            return
        }

        val playerId = player.uniqueId
        val armorStandId = playerToSeatMap[playerId]!!
        val standLocation = player.location.clone().add(0.0, 1.0, 0.0)

        player.teleport(standLocation)
        player.sendActionBar(Component.text(" "))
        player.playSitSound()

        playerToSitLocationMap.remove(playerId)
        cleanArmorStand(armorStandId)
    }

    override fun getSeat(player: Player): Entity? {
        playerToSeatMap[player.uniqueId] ?: return null
        return server.getEntity(playerToSeatMap[player.uniqueId]!!)
    }

    override fun getSitLocation(player: Player): Location? {
        return playerToSitLocationMap[player.uniqueId]
    }

    override fun getSitterByLocation(location: Location): Player? {
        return playerToSitLocationMap.entries
            .firstOrNull { it.value == location.viewAligned() }?.key
            ?.let { Bukkit.getPlayer(it) }
    }

    override fun getSitterBySeat(seat: Entity): Player? {
        return playerToSeatMap.entries
            .firstOrNull { it.value == seat.uniqueId }?.key
            ?.let { Bukkit.getPlayer(it) }
    }

    override fun isSeat(entity: Entity): Boolean {
        return playerToSeatMap.containsValue(entity.uniqueId)
    }

    override fun isSitLocation(location: Location): Boolean {
        return playerToSitLocationMap.containsValue(location.viewAligned())
    }

    override fun standAll() {
        playerToSitLocationMap.keys.forEach {
            val player = server.getPlayer(it)
            player!!.stand()
        }
    }

    private fun cleanArmorStand(uuid: UUID) {
        if (!playerToSeatMap.containsValue(uuid)) {
            return
        }

        val armorStand = server.getEntity(uuid) ?: return
        armorStand.remove()

        playerToSeatMap.entries.removeIf { it.value == uuid }
    }
}
