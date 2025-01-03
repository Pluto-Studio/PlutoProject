package ink.pmc.menu.prebuilt.page

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.text
import ink.pmc.framework.chat.mochaText
import ink.pmc.menu.api.dsl.pageDescriptor
import org.bukkit.Material

val HOME_PAGE_DESCRIPTOR = pageDescriptor {
    id = "menu:home"
    icon = Material.CAMPFIRE
    name = component {
        text("主页") with mochaText
    }
}