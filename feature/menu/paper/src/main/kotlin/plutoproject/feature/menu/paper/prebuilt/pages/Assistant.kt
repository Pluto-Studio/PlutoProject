package plutoproject.feature.menu.paper.prebuilt.pages

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.text
import org.bukkit.Material
import plutoproject.feature.menu.api.paper.dsl.PageDescriptor
import plutoproject.foundation.common.text.mochaText

val AssistantPageDescriptor = PageDescriptor {
    id = "menu:assistant"
    icon = Material.TRIPWIRE_HOOK
    name = component {
        text("辅助功能") with mochaText
    }
}
