package plutoproject.feature.menu.paper.prebuilt.pages

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.text
import org.bukkit.Material
import plutoproject.feature.menu.api.paper.dsl.PageDescriptor
import plutoproject.foundation.common.text.mochaText

val HomePageDescriptor = PageDescriptor {
    id = "menu:home"
    icon = Material.CAMPFIRE
    name = component {
        text("主页") with mochaText
    }
}
