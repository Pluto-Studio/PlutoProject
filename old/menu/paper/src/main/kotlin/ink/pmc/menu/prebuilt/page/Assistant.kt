package ink.pmc.menu.prebuilt.page

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.text
import ink.pmc.framework.chat.mochaText
import ink.pmc.menu.api.dsl.pageDescriptor
import org.bukkit.Material

val ASSISTANT_PAGE_DESCRIPTOR = pageDescriptor {
    id = "menu:assistant"
    icon = Material.TRIPWIRE_HOOK
    name = component {
        text("辅助功能") with mochaText
    }
}