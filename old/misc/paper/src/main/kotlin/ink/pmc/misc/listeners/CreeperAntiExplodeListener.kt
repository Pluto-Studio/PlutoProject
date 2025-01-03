package ink.pmc.misc.listeners

import com.catppuccin.Palette
import ink.pmc.misc.MiscConfig
import ink.pmc.framework.chat.bukkitColor
import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.EntityType
import org.bukkit.entity.Firework
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.persistence.PersistentDataType

@Suppress("UNUSED")
object CreeperAntiExplodeListener : Listener {

    private val fireworkKey = NamespacedKey("misc", "creeper_explode_firework")

    private val fireworkColors = arrayOf(
        Palette.MOCHA.rosewater.bukkitColor,
        Palette.MOCHA.flamingo.bukkitColor,
        Palette.MOCHA.pink.bukkitColor,
        Palette.MOCHA.mauve.bukkitColor,
        Palette.MOCHA.red.bukkitColor,
        Palette.MOCHA.maroon.bukkitColor,
        Palette.MOCHA.peach.bukkitColor,
        Palette.MOCHA.yellow.bukkitColor,
        Palette.MOCHA.green.bukkitColor,
        Palette.MOCHA.teal.bukkitColor,
        Palette.MOCHA.sky.bukkitColor,
        Palette.MOCHA.sapphire.bukkitColor,
        Palette.MOCHA.blue.bukkitColor,
        Palette.MOCHA.lavender.bukkitColor,
    )

    @EventHandler
    fun EntityExplodeEvent.e() {
        if (entity.type != EntityType.CREEPER) {
            return
        }

        blockList().clear()

        if (MiscConfig.creeperAntiExplodeFirework) {
            entity.location.launchFirework()
        }
    }

    @EventHandler
    fun EntityDamageByEntityEvent.e() {
        if (damager.type != EntityType.FIREWORK_ROCKET) {
            return
        }

        if (!damager.persistentDataContainer.has(fireworkKey)) {
            return
        }

        isCancelled = true
    }

    private fun Location.launchFirework() {
        val firework = world.spawnEntity(this.add(0.0, 1.0, 0.0), EntityType.FIREWORK_ROCKET) as Firework
        val meta = firework.fireworkMeta
        val colors = mutableSetOf<Color>()

        repeat(3) {
            var color = fireworkColors.random()
            while (colors.contains(color)) {
                color = fireworkColors.random()
            }
            colors.add(color)
        }

        meta.power = 1
        meta.addEffects(FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE).withColor(colors).build())

        firework.fireworkMeta = meta
        firework.persistentDataContainer.set(fireworkKey, PersistentDataType.BOOLEAN, true)
        firework.detonate()
    }

}