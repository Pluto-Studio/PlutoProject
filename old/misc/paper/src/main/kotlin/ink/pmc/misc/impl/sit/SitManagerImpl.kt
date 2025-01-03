package ink.pmc.misc.impl.sit

import ink.pmc.framework.entity.entity
import ink.pmc.framework.player.bukkitPlayer
import ink.pmc.framework.player.threadSafeTeleport
import ink.pmc.framework.world.eraseAngle
import ink.pmc.misc.*
import ink.pmc.misc.api.sit.SitManager
import ink.pmc.misc.api.sit.isSitting
import ink.pmc.misc.api.sit.sitter
import ink.pmc.misc.api.sit.stand
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.block.data.type.Campfire
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.*

class SitManagerImpl : SitManager {
    private val playerToSitLocationMap = mutableMapOf<UUID, Location>()
    private val playerToSeatMap = mutableMapOf<UUID, UUID>()
    override val sitters: Set<Player>
        get() = playerToSitLocationMap.keys.map { it.bukkitPlayer!! }.toSet()
    override val seats: Set<Entity>
        get() = playerToSeatMap.values.map { it.entity!! }.toSet()

    override fun sit(player: Player, location: Location) {
        if (player.isSitting) {
            player.stand()
        }

        var sitLoc = location.eraseAngle()

        if (!checkLocation(location)) {
            val tryFind = findLegalLocation(sitLoc)

            if (tryFind == null) {
                player.sendMessage(ILLEGAL_LOC)
                return
            }

            sitLoc = tryFind
        }

        if (sitLoc.sitter != null) {
            player.showTitle(MULTI_SITTERS_TITLE)
            player.playSound(MULTI_SITTERS_SOUND)
            return
        }

        if (sitLoc.block.blockData is Campfire && !isExtinctCampfire(sitLoc.block)) {
            player.sendMessage(ILLEGAL_LOC)
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

        /*
        * 在 Paper 上通过 PlayerQuitEvent 触发此处时，
        * 若进行 teleportAsync 会由于异步延迟而无法正常传送。
        * 同时，Folia 要求必须 teleportAsync，并且在事件中也可以正常工作，
        * 因此使用自适应安全的传送方法。
        * */
        player.threadSafeTeleport(standLocation)
        player.sendActionBar(Component.text(" "))
        player.playSitSound()

        playerToSitLocationMap.remove(playerId)
        cleanArmorStand(armorStandId)
    }

    override fun getSeat(player: Player): Entity? {
        playerToSeatMap[player.uniqueId] ?: return null
        return plugin.server.getEntity(playerToSeatMap[player.uniqueId]!!)
    }

    override fun getSitLocation(player: Player): Location? {
        return playerToSitLocationMap[player.uniqueId]
    }

    override fun getSitterByLocation(location: Location): Player? {
        return playerToSitLocationMap.entries.firstOrNull { it.value == location.eraseAngle() }?.key?.bukkitPlayer
    }

    override fun getSitterBySeat(seat: Entity): Player? {
        return playerToSeatMap.entries.firstOrNull { it.value == seat.uniqueId }?.key?.bukkitPlayer
    }

    override fun isSeat(entity: Entity): Boolean {
        return playerToSeatMap.containsValue(entity.uniqueId)
    }

    override fun isSitLocation(location: Location): Boolean {
        return playerToSitLocationMap.containsValue(location.eraseAngle())
    }

    override fun standAll() {
        playerToSitLocationMap.keys.forEach {
            val player = plugin.server.getPlayer(it)
            player!!.stand()
        }
    }

    private fun cleanArmorStand(uuid: UUID) {
        if (!playerToSeatMap.containsValue(uuid)) {
            return
        }

        val armorStand = plugin.server.getEntity(uuid) ?: return
        armorStand.remove()

        playerToSeatMap.entries.removeIf { it.value == uuid }
    }
}