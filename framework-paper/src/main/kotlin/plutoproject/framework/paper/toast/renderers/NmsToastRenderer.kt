package plutoproject.framework.paper.toast.renderers

import net.kyori.adventure.text.Component
import net.minecraft.advancements.*
import net.minecraft.advancements.critereon.ImpossibleTrigger
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket
import net.minecraft.resources.ResourceLocation
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import plutoproject.framework.common.util.data.emptyOptionalOf
import plutoproject.framework.common.util.data.optionalOf
import plutoproject.framework.paper.api.toast.Toast
import plutoproject.framework.paper.api.toast.ToastRenderer
import plutoproject.framework.paper.api.toast.ToastType
import plutoproject.framework.paper.util.chat.component.toNms
import plutoproject.framework.paper.util.entity.sendPacket
import plutoproject.framework.paper.util.resourceLocationOf

@Suppress("UNUSED")
open class NmsToastRenderer : ToastRenderer<Player> {
    private val location = optionalOf(resourceLocationOf("visual", "paper_toast_renderer"))
    private val criteria = mapOf("for_free" to Criterion(ImpossibleTrigger(), ImpossibleTrigger.TriggerInstance()))
    private val fixedRequirements = listOf("for_free")
    private val requirements = AdvancementRequirements(listOf(fixedRequirements))
    private val adventureTexture =
        optionalOf(resourceLocationOf("minecraft:textures/gui/advancements/backgrounds/adventure.png"))
    private val progress = mapOf(location.get() to AdvancementProgress().apply {
        update(requirements)
        getCriterion("for_free")?.grant()
    })

    private val ToastType.nms: AdvancementType
        get() {
            return when (this) {
                ToastType.TASK -> AdvancementType.TASK
                ToastType.CHALLENGE -> AdvancementType.CHALLENGE
                ToastType.GOAL -> AdvancementType.GOAL
            }
        }

    override fun render(player: Player, obj: Toast) {
        val item = ItemStack(obj.icon, 1)
        val display = optionalOf(
            DisplayInfo(
                CraftItemStack.asNMSCopy(item),
                obj.message.toNms(),
                Component.text("PlutoProject Visual - Paper Toast Renderer").toNms(),
                optionalOf(resourceLocationOf(obj.frame.texture)),
                obj.type.nms,
                true,
                false,
                true,
            )
        )

        val advancement =
            Advancement(emptyOptionalOf(), display, AdvancementRewards.EMPTY, criteria, requirements, false)
        val holder = AdvancementHolder(location.get(), advancement)
        val displayPacket = ClientboundUpdateAdvancementsPacket(false, listOf(holder), emptySet(), progress)
        player.sendPacket(displayPacket)

        val removed = setOf(location.get())
        val removePacket = ClientboundUpdateAdvancementsPacket(
            false,
            emptyList(),
            removed,
            mapOf<ResourceLocation, AdvancementProgress>()
        )
        player.sendPacket(removePacket)
    }
}
