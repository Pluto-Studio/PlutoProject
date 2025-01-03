package ink.pmc.essentials.listeners

import ink.pmc.essentials.IF_PROTECTED_ACTION
import ink.pmc.essentials.IF_UNFINISHED_BOOK
import ink.pmc.essentials.IF_UNFINISHED_BOOK_AUTHOR
import ink.pmc.essentials.ITEMFRAME_PROTECT_BYPASS
import ink.pmc.essentials.commands.isProtected
import ink.pmc.essentials.commands.protector
import ink.pmc.essentials.commands.protectorName
import ink.pmc.framework.chat.replace
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

internal val Material.isOpenableBook: Boolean
    get() = this == Material.WRITTEN_BOOK || this == Material.WRITABLE_BOOK

@Suppress("UNUSED", "UnusedReceiverParameter")
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
                        .title(IF_UNFINISHED_BOOK)
                        .author(IF_UNFINISHED_BOOK_AUTHOR)
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
        if (itemFrame.protector == player || player.hasPermission(ITEMFRAME_PROTECT_BYPASS)) return

        player.sendActionBar(IF_PROTECTED_ACTION.replace("<player>", itemFrame.protectorName))
        isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun HangingBreakByEntityEvent.e() {
        if (entity !is ItemFrame) return
        val frame = entity as ItemFrame

        if (!frame.isProtected) return
        if (remover !is Player) return
        if (frame.protector == remover || remover.hasPermission(ITEMFRAME_PROTECT_BYPASS)) return

        remover.sendActionBar(IF_PROTECTED_ACTION.replace("<player>", frame.protectorName))
        isCancelled = true
    }
}