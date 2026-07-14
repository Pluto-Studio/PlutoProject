package plutoproject.feature.creeperfirework.paper

import com.catppuccin.Palette
import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.EntityType
import org.bukkit.entity.Firework
import org.bukkit.persistence.PersistentDataType
import plutoproject.foundation.paper.text.toBukkitColor
import plutoproject.kernel.api.currentModuleContext
import plutoproject.kernel.api.paper.PaperModuleContext

internal val fireworkKey = NamespacedKey((currentModuleContext() as PaperModuleContext).plugin, "creeper_firework")

private val fireworkColors = arrayOf(
    Palette.MOCHA.rosewater.toBukkitColor(),
    Palette.MOCHA.flamingo.toBukkitColor(),
    Palette.MOCHA.pink.toBukkitColor(),
    Palette.MOCHA.mauve.toBukkitColor(),
    Palette.MOCHA.red.toBukkitColor(),
    Palette.MOCHA.maroon.toBukkitColor(),
    Palette.MOCHA.peach.toBukkitColor(),
    Palette.MOCHA.yellow.toBukkitColor(),
    Palette.MOCHA.green.toBukkitColor(),
    Palette.MOCHA.teal.toBukkitColor(),
    Palette.MOCHA.sky.toBukkitColor(),
    Palette.MOCHA.sapphire.toBukkitColor(),
    Palette.MOCHA.blue.toBukkitColor(),
    Palette.MOCHA.lavender.toBukkitColor(),
)

internal fun Location.launchFirework() {
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
