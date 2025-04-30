package plutoproject.feature.paper.home

import ink.pmc.advkt.component.*
import ink.pmc.advkt.sound.key
import ink.pmc.advkt.sound.sound
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.Location
import plutoproject.feature.paper.api.home.Home
import plutoproject.feature.paper.api.home.HomeManager
import plutoproject.framework.common.util.chat.palettes.*
import plutoproject.framework.paper.api.worldalias.aliasOrName

val COMMAND_SETHOME_FAILED_REACH_LIMIT
    get() = component {
        text("你当前设置的家数量已经到达上限，请删除一些再试") with mochaMaroon
        newline()
        text("当前家上限数量为 ") with mochaSubtext0
        text("${HomeManager.maxHomes} ") with mochaText
        text("个") with mochaSubtext0
    }

val COMMAND_SETHOME_FAILED_EXISTED = component {
    text("你已经有一个名为 ") with mochaMaroon
    text("<name> ") with mochaText
    text("的家了") with mochaMaroon
    newline()
    text("请删除或更换一个名字后再试") with mochaSubtext0
}

val COMMAND_SETHOME_FAILED_NOT_VALID = component {
    text("家的名字只可以包含字母、数字、下划线") with mochaMaroon
    newline()
    text("不可以使用中文、空格等字符") with mochaSubtext0
}

val COMMAND_SETHOME_FAILED_LENGTN_LIMIT
    get() = component {
        text("家的名字最多只能使用 ") with mochaMaroon
        text("${HomeManager.nameLengthLimit} ") with mochaText
        text("个字符") with mochaMaroon
        newline()
        text("请缩短一些后再试") with mochaSubtext0
    }

val COMMAND_SETHOME_SUCCEED = component {
    text("已设置名为 ") with mochaPink
    text("<name> ") with mochaText
    text("的家") with mochaPink
}

val COMMAND_HOME_NOT_EXISTED = component {
    text("名为 ") with mochaMaroon
    text("<name> ") with mochaText
    text("的家不存在") with mochaMaroon
}

val COMMAND_EDITHOME_ALREADY_PREFERRED = component {
    text("名为 ") with mochaMaroon
    text("<name> ") with mochaText
    text("的家已经是首选了") with mochaMaroon
}

val COMMAND_EDITHOME_PREFER_SUCCEED = component {
    text("已将名为 ") with mochaPink
    text("<name> ") with mochaText
    text("的家设为首选") with mochaPink
}

val COMMAND_EDITHOME_ALREADY_STARRED = component {
    text("名为 ") with mochaMaroon
    text("<name> ") with mochaText
    text("的家已经收藏过了") with mochaMaroon
}

val COMMAND_EDITHOME_STAR_SUCCEED = component {
    text("已将名为 ") with mochaPink
    text("<name> ") with mochaText
    text("的家收藏") with mochaPink
}

val COMMAND_EDITHOME_RENAME_SUCCEED = component {
    text("已将该家更名为 ") with mochaPink
    text("<new_name>") with mochaText
}

val COMMAND_EDITHOME_MOVE_SUCCEED = component {
    text("已将家 ") with mochaPink
    text("<name> ") with mochaText
    text("迁移到你所在的位置") with mochaPink
}

val COMMAND_HOME_NOT_EXISTED_UUID = component {
    text("无法通过指定的 ID 找到对应的家") with mochaMaroon
}

val COMMAND_HOME_SUCCEED = component {
    text("已传送到名为 ") with mochaPink
    text("<name> ") with mochaText
    text("的家") with mochaPink
}

val COMMAND_DELHOME_SUCCEED = component {
    text("已删除名为 ") with mochaPink
    text("<name> ") with mochaText
    text("的家") with mochaPink
}

val COMMAND_HOMES_PLAYER_NOT_FOUND = component {
    text("未找到玩家 ") with mochaMaroon
    text("<name> ") with mochaText
    text("的数据，请检查玩家名是否正确") with mochaMaroon
}

val COMMAND_HOMES_PLAYER_HAS_NO_HOME = component {
    text("玩家 ") with mochaMaroon
    text("<player> ") with mochaText
    text("没有设置家") with mochaMaroon
}

private val UI_HOME_EMPTY_PROMPT = component {
    text("点击下方按钮或 ") with mochaSubtext0 without italic()
    text("/sethome ") with mochaLavender without italic()
    text("以留下你的足迹") with mochaSubtext0 without italic()
}

val UI_HOME_EMPTY_LORE = listOf(
    UI_HOME_EMPTY_PROMPT
)

val UI_HOME_EMPTY_LORE_OTHER = listOf(
    component { text("该玩家未设置家") with mochaSubtext0 without italic() }
)

val UI_HOME_TITLE = component {
    text("<player> 的家")
}

val UI_HOME_TITLE_SELF = component {
    text("家")
}

val UI_HOME_ITEM_NAME = component {
    text("<name>") with mochaYellow without italic()
}

private val UI_HOME_PREFERRED = component {
    text("√ 首选的家") with mochaGreen without italic()
}

private val UI_HOME_STARRED = component {
    text("✨ 收藏的家") with mochaYellow without italic()
}

private val UI_HOME_ITEM_LORE_LOC = component {
    text("<world> <x>, <y>, <z>") with mochaSubtext0 without italic()
}

fun getUIHomeButtonLore(home: Home): List<Component> {
    val loc = home.location
    return mutableListOf<Component>().apply {
        add(component {
            raw(
                UI_HOME_ITEM_LORE_LOC
                    .replace("<world>", loc.world.aliasOrName)
                    .replace("<x>", "${loc.blockX}")
                    .replace("<y>", "${loc.blockY}")
                    .replace("<z>", "${loc.blockZ}")
            )
        })
        if (home.isPreferred) add(UI_HOME_PREFERRED)
        if (home.isStarred) add(UI_HOME_STARRED)
        add(Component.empty())
        add(component {
            text("左键 ") with mochaLavender without italic()
            text("传送到该位置") with mochaText without italic()
        })
        add(component {
            text("右键 ") with mochaLavender without italic()
            text("编辑家") with mochaText without italic()
        })
    }
}

val UI_HOME_EDITOR_TITLE = component {
    text("编辑 <name>")
}

val UI_HOME_RENAME = component {
    text("重命名") with mochaText without italic()
}

val UI_HOME_RENAME_LORE = listOf(
    Component.empty(),
    component {
        text("左键 ") with mochaLavender without italic()
        text("重命名该家") with mochaText without italic()
    }
)

val UI_HOME_CHANGE_LOCATION = component {
    text("迁移") with mochaText without italic()
}

val UI_HOME_CHANGE_LOCATION_LORE = listOf(
    Component.empty(),
    component {
        text("左键 ") with mochaLavender without italic()
        text("将该家迁移到你所在的位置") with mochaText without italic()
    }
)

val UI_HOME_DELETE = component {
    text("删除") with mochaText without italic()
}

val UI_HOME_DELETE_LORE = listOf(
    component { text("该操作不可撤销") with mochaRed without italic() },
    Component.empty(),
    component {
        text("Shift + 左键 ") with mochaLavender without italic()
        text("删除该家") with mochaText without italic()
    }
)

val UI_HOME_EDIT_SUCCEED = component {
    text("√ 已保存") with mochaGreen without italic()
}

val UI_HOME_PREFER = component {
    text("设为首选") with mochaText without italic()
}

val UI_HOME_PREFER_LORE = listOf(
    component { text("设为首选后，") with mochaSubtext0 without italic() },
    component {
        text("使用 ") with mochaSubtext0 without italic()
        text("/home ") with mochaLavender without italic()
        text("或「手账」将默认传送该家") with mochaSubtext0 without italic()
    },
    Component.empty(),
    component {
        text("左键 ") with mochaLavender without italic()
        text("将该家设为首选") with mochaText without italic()
    }
)

val UI_HOME_PREFER_UNSET = component {
    text("取消首选") with mochaText without italic()
}


val UI_HOME_PREFER_UNSET_LORE = listOf(
    Component.empty(),
    component {
        text("左键 ") with mochaLavender without italic()
        text("将该家取消首选") with mochaText without italic()
    }
)

val UI_HOME_STAR = component {
    text("收藏") with mochaText without italic()
}

val UI_HOME_STAR_LORE = listOf(
    component { text("收藏后，该家将靠前显示") with mochaSubtext0 without italic() },
    Component.empty(),
    component {
        text("左键 ") with mochaLavender without italic()
        text("将该家收藏") with mochaText without italic()
    }
)

val UI_HOME_STAR_UNSET = component {
    text("取消收藏") with mochaText without italic()
}

val UI_HOME_STAR_UNSET_LORE = listOf(
    Component.empty(),
    component {
        text("左键 ") with mochaLavender without italic()
        text("将该家取消收藏") with mochaText without italic()
    }
)

val UI_HOME_EDIT_SUCCEED_SOUND = sound {
    key(Key.key("block.note_block.bell"))
}

val UI_HOME_EDITOR_REMOVE_SOUND = sound {
    key(Key.key("block.decorated_pot.break"))
}

val UI_HOME_EDITOR_RENAME_TITLE = component {
    text("重命名 <name>")
}

val UI_HOME_EDITOR_RENAME_EXIT_LORE = listOf(
    Component.empty(),
    component {
        text("左键 ") with mochaLavender without italic()
        text("退出编辑") with mochaText without italic()
    }
)

fun getUIHomeEditorRenameSaveButtonLore(home: Home): List<Component> {
    val loc = home.location
    return listOf(
        component {
            raw(
                UI_HOME_ITEM_LORE_LOC
                    .replace("<world>", loc.world.aliasOrName)
                    .replace("<x>", "${loc.blockX}")
                    .replace("<y>", "${loc.blockY}")
                    .replace("<z>", "${loc.blockZ}")
            )
        },
        Component.empty(),
        component {
            text("左键 ") with mochaLavender without italic()
            text("保存并退出") with mochaText without italic()
        }
    )
}

val UI_HOME_EDITOR_RENAME_SAVE_INVALID_LORE = listOf(
    Component.empty(),
    component { text("仅可使用字母、数字、下划线") with mochaMaroon without italic() }
)

val UI_HOME_EDITOR_RENAME_SAVE_TOO_LONG
    get() = listOf(
        Component.empty(),
        component {
            text("名称过长，最多使用 ") with mochaMaroon without italic()
            text("${HomeManager.nameLengthLimit} ") with mochaText without italic()
            text("个字符") with mochaMaroon without italic()
        }
    )

val UI_HOME_EDITOR_RENAME_SAVE_EXISTED = listOf(
    Component.empty(),
    component {
        component { text("已存在同名的家") with mochaMaroon without italic() }
    }
)

val UI_HOME_EDITOR_RENAME_SAVED = listOf(
    Component.empty(),
    UI_HOME_EDIT_SUCCEED
)

val UI_HOME_EDITOR_RENAME_INVALID_SOUND = sound {
    key(Key.key("block.note_block.didgeridoo"))
}

val UI_HOME_CREATOR_TITLE = component {
    text("创建家")
}

val UI_HOME_CREATOR_LEFT_LORE = listOf(
    Component.empty(),
    component {
        text("左键 ") with mochaLavender without italic()
        text("返回上一页") with mochaText without italic()
    }
)

const val UI_HOME_CREATOR_INPUT = "输入名称..."

fun getUIHomeCreatorOutputButtonLore(loc: Location): List<Component> {
    return listOf(
        component {
            raw(
                UI_HOME_ITEM_LORE_LOC
                    .replace("<world>", loc.world.aliasOrName)
                    .replace("<x>", "${loc.blockX}")
                    .replace("<y>", "${loc.blockY}")
                    .replace("<z>", "${loc.blockZ}")
            )
        },
        Component.empty(),
        component {
            text("左键 ") with mochaLavender without italic()
            text("创建家") with mochaText without italic()
        }
    )
}

val UI_HOME_VIEWER_CREATE = component {
    text("创建家") with mochaText without italic()
}

val UI_HOME_VIEWER_CREATE_LORE = listOf(
    Component.empty(),
    component {
        text("左键 ") with mochaLavender without italic()
        text("在当前位置创建家") with mochaText without italic()
    }
)
