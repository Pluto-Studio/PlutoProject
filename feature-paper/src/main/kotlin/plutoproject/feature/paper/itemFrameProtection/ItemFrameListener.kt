package plutoproject.feature.paper.itemFrameProtection

import io.papermc.paper.event.player.PlayerItemFrameChangeEvent
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent.ItemFrameChangeAction
import net.kyori.adventure.inventory.Book
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.inventory.meta.WritableBookMeta
import plutoproject.framework.common.util.chat.component.replace

internal val Material.isOpenableBook: Boolean
    get() = this == Material.WRITTEN_BOOK || this == Material.WRITABLE_BOOK

@Suppress("UNUSED")
object ItemFrameListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun PlayerItemFrameChangeEvent.e() {
        val item = itemFrame.item

        if (item.type.isOpenableBook && action == ItemFrameChangeAction.ROTATE && !player.isSneaking) {
            when (item.type) {
                Material.WRITTEN_BOOK -> player.openBook(item)
                Material.WRITABLE_BOOK -> {
                    val meta = item.itemMeta as WritableBookMeta
                    val contents = meta.pages.map { Component.text(it) }
                    val book = Book.builder()
                        .title(ITEMFRAME_PROTECTION_UNFINISHED_BOOK)
                        .author(ITEMFRAME_PROTECTION_UNKNOWN_AUTHOR)
                        .pages(contents)
                        .build()
                    player.openBook(book)
                }

                else -> {
                    error("Unsupported book type: ${item.type}")
                }
            }
            isCancelled = true
            return
        }

        if (!itemFrame.isProtected) return
        if (itemFrame.protector == player || player.hasPermission(ITEMFRAME_PROTECTION_BYPASS_PERMISSION)) return

        player.sendActionBar(ITEMFRAME_PROTECTED.replace("<player>", itemFrame.protectorName))
        isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun HangingBreakByEntityEvent.e() {
        if (entity !is ItemFrame) return
        val frame = entity as ItemFrame

        if (!frame.isProtected) return
        if (remover !is Player) return
        if (frame.protector == remover || remover.hasPermission(ITEMFRAME_PROTECTION_BYPASS_PERMISSION)) return

        remover.sendActionBar(ITEMFRAME_PROTECTED.replace("<player>", frame.protectorName))
        isCancelled = true
    }
}
