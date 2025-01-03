package plutoproject.framework.paper.util

import com.destroystokyo.paper.profile.ProfileProperty
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.profile.PlayerTextures.SkinModel
import java.net.URL
import java.util.*

fun ItemStack.setHeadTexture(
    skin: URL,
    model: SkinModel = SkinModel.CLASSIC
): ItemStack = apply {
    editMeta {
        it as SkullMeta
        it.playerProfile = Bukkit.createProfile(UUID.randomUUID(), "").apply {
            textures.setSkin(skin, model)
        }
    }
}

fun ItemStack.setHeadTexture(value: String): ItemStack = apply {
    editMeta {
        it as SkullMeta
        it.playerProfile = Bukkit.createProfile(UUID.randomUUID(), "").apply {
            properties.add(ProfileProperty("textures", value))
        }
    }
}
