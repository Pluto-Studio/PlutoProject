package plutoproject.feature.gallery.adapter.paper

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.empty
import ink.pmc.advkt.component.keybind
import ink.pmc.advkt.component.text
import ink.pmc.advkt.component.translatable
import plutoproject.framework.common.util.chat.palettes.mochaFlamingo
import plutoproject.framework.common.util.chat.palettes.mochaLavender
import plutoproject.framework.common.util.chat.palettes.mochaSubtext0
import plutoproject.framework.common.util.chat.palettes.mochaText

val IMAGE_ITEM_NAME = component {
    text("<name> ") with mochaText
    text("(<width> × <height>)") with mochaLavender
}

val IMAGE_ITEM_LORE = listOf(
    component { text("由 <creator>") with mochaSubtext0 },
    component { text("创建于 <time>") with mochaSubtext0 },
    component { empty() },
    component { text("这是一幅地图画！") with mochaFlamingo },
    component {
        text("你可以把它放入有 ") with mochaText
        text("<width> × <height> ") with mochaLavender
        text("大小的展示框墙内") with mochaText
    },
    component { empty() },
    component {
        keybind("key.use") with mochaLavender
        text(" 放入展示框") with mochaText
    }
)
