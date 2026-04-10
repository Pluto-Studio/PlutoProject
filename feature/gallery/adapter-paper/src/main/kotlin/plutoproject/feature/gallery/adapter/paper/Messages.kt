package plutoproject.feature.gallery.adapter.paper

import ink.pmc.advkt.component.*
import plutoproject.framework.common.util.chat.palettes.*

val IMAGE_ITEM_NAME = component {
    text("<name> ") with mochaText
    text("(<width> × <height>)") with mochaLavender
}

val IMAGE_ITEM_LORE = listOf(
    component { text("由 <creator>") with mochaSubtext0 without italic() },
    component { text("创建于 <time>") with mochaSubtext0 without italic() },
    component { empty() },
    component { text("这是一幅地图画！") with mochaFlamingo without italic() },
    component {
        text("你可以把它放入一面 ") with mochaText without italic()
        text("<width> × <height> ") with mochaLavender without italic()
        text("的展示框内") with mochaText without italic()
    },
    component { empty() },
    component {
        keybind("key.use") with mochaLavender without italic()
        text(" 放入展示框") with mochaText without italic()
    }
)

val IMAGE_ITEM_PLACEMENT_FAILED_INVALID = component {
    newline()
    text("这似乎是一幅无效的地图画...") with mochaMaroon
    newline()
    text("无法找到此物品对应的地图画数据，可能是因为它已被删除。") with mochaSubtext0
    newline()
}

val IMAGE_ITEM_PLACEMENT_FAILED_NO_SPACE_SUBTITLE = component {
    text("空间不足，需要一面 ") with mochaMaroon
    text("<width> × <height> ") with mochaText
    text("的展示框") with mochaMaroon
}
