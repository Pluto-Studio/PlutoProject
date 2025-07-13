package plutoproject.feature.paper.home

import ink.pmc.advkt.component.*
import net.kyori.adventure.text.Component
import org.bukkit.Location
import plutoproject.feature.paper.api.home.Home
import plutoproject.feature.paper.api.home.HomeManager
import plutoproject.framework.common.util.chat.palettes.*
import plutoproject.framework.paper.api.worldalias.aliasOrName

val COMMAND_SETHOME_FAILED_AMOUNT_LIMIT
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

val COMMAND_SETHOME_FAILED_NAME_NOT_VALID = component {
    text("家的名字只可以包含字母、数字、下划线") with mochaMaroon
    newline()
    text("不可以使用中文、空格等字符") with mochaSubtext0
}

val COMMAND_SETHOME_FAILED_NAME_LENGTH_LIMIT
    get() = component {
        text("家的名字最多只能使用 ") with mochaMaroon
        text("${HomeManager.nameLengthLimit} ") with mochaText
        text("个字符") with mochaMaroon
        newline()
        text("请缩短一些后再试") with mochaSubtext0
    }

val COMMAND_SETHOME = component {
    text("已设置名为 ") with mochaPink
    text("<name> ") with mochaText
    text("的家") with mochaPink
}

val COMMAND_HOME_FAILED_NOT_EXISTED = component {
    text("名为 ") with mochaMaroon
    text("<name> ") with mochaText
    text("的家不存在") with mochaMaroon
}

val COMMAND_EDITHOME_FAILED_ALREADY_PREFERRED = component {
    text("名为 ") with mochaMaroon
    text("<name> ") with mochaText
    text("的家已经是首选了") with mochaMaroon
}

val COMMAND_EDITHOME_PREFER = component {
    text("已将名为 ") with mochaPink
    text("<name> ") with mochaText
    text("的家设为首选") with mochaPink
}

val COMMAND_EDITHOME_FAILED_ALREADY_STARRED = component {
    text("名为 ") with mochaMaroon
    text("<name> ") with mochaText
    text("的家已经收藏过了") with mochaMaroon
}

val COMMAND_EDITHOME_STAR = component {
    text("已将名为 ") with mochaPink
    text("<name> ") with mochaText
    text("的家收藏") with mochaPink
}

val COMMAND_EDITHOME_RENAME = component {
    text("已将该家更名为 ") with mochaPink
    text("<new_name>") with mochaText
}

val COMMAND_EDITHOME_MOVE = component {
    text("已将家 ") with mochaPink
    text("<name> ") with mochaText
    text("迁移到你所在的位置") with mochaPink
}

val COMMAND_HOME = component {
    text("已传送到名为 ") with mochaPink
    text("<name> ") with mochaText
    text("的家") with mochaPink
}

val COMMAND_DELHOME = component {
    text("已删除名为 ") with mochaPink
    text("<name> ") with mochaText
    text("的家") with mochaPink
}

val COMMAND_HOMES_FAILED_PLAYER_NOT_FOUND = component {
    text("未找到玩家 ") with mochaMaroon
    text("<name> ") with mochaText
    text("的数据，请检查玩家名是否正确") with mochaMaroon
}

val COMMAND_HOMES_FAILED_PLAYER_HAS_NO_HOME = component {
    text("玩家 ") with mochaMaroon
    text("<player> ") with mochaText
    text("没有设置家") with mochaMaroon
}

private val UI_HOME_MENU_EMPTY = component {
    text("点击下方按钮或 ") with mochaSubtext0
    text("/sethome ") with mochaLavender
    text("以留下你的足迹") with mochaSubtext0
}

val UI_HOME_LIST_EMPTY_LORE = listOf(
    UI_HOME_MENU_EMPTY
)

val UI_HOME_LIST_EMPTY_LORE_OTHER = listOf(
    component { text("该玩家未设置家") with mochaSubtext0 }
)

val UI_HOME_LIST_TITLE = component {
    text("<player> 的家")
}

val UI_HOME_LIST_TITLE_SELF = component {
    text("家")
}

val UI_HOME_LIST_HOME_NAME = component {
    text("<name>") with mochaYellow
}

val UI_HOME_LIST_HOME_PREFERRED = component {
    text("√ 首选的家") with mochaGreen
}

val UI_HOME_LIST_HOME_STARRED = component {
    text("✨ 收藏的家") with mochaYellow
}

private val UI_HOME_LIST_HOME_LOCATION = component {
    text("<world> <x>, <y>, <z>") with mochaSubtext0
}

fun getUIHomeListHomeLore(home: Home): List<Component> {
    val loc = home.location
    return mutableListOf<Component>().apply {
        add(component {
            raw(
                UI_HOME_LIST_HOME_LOCATION
                    .replace("<world>", loc.world.aliasOrName)
                    .replace("<x>", "${loc.blockX}")
                    .replace("<y>", "${loc.blockY}")
                    .replace("<z>", "${loc.blockZ}")
            )
        })
        if (home.isPreferred) add(UI_HOME_LIST_HOME_PREFERRED)
        if (home.isStarred) add(UI_HOME_LIST_HOME_STARRED)
        add(Component.empty())
        add(component {
            text("左键 ") with mochaLavender
            text("传送到该位置") with mochaText
        })
        add(component {
            text("右键 ") with mochaLavender
            text("编辑家") with mochaText
        })
    }
}

val UI_HOME_NAME_DIALOG_CANCEL = component {
    text("取消")
}

val UI_HOME_NAME_DIALOG_SUBMIT = component {
    text("提交")
}

val UI_HOME_NAME_DIALOG_TEXT_INPUT_LABEL = component {
    text("输入家名称 ") with mochaText
    text("(最长 ") with mochaSubtext0
    text("${HomeManager.nameLengthLimit} ") with mochaLavender
    text("个字符)") with mochaSubtext0
}

val UI_HOME_CREATE_DIALOG_TITLE = component {
    text("创建家") with mochaText
}

val UI_HOME_RENAME_DIALOG_TITLE = component {
    text("编辑家名称") with mochaText
}

val UI_HOME_RENAME_DIALOG_RENAMING = component {
    text("正在修改家 ") with mochaText
    text("<name> ") with mochaYellow
    text("的名称") with mochaText
}

val UI_HOME_DIALOG_SAVE_FAILED_EMPTY_NAME = component {
    text("请输入名称") with mochaMaroon
}

val UI_HOME_DIALOG_SAVE_FAILED_TOO_LONG = component {
    text("名称过长，最多使用 ") with mochaMaroon
    text("${HomeManager.nameLengthLimit} ") with mochaText
    text("个字符") with mochaMaroon
}

val UI_HOME_DIALOG_SAVE_FAILED_EXISTED = component {
    text("已存在同名的家") with mochaMaroon
}

val UI_HOME_DIALOG_SAVED = component {
    text("√ 已保存") with mochaGreen
}

val UI_HOME_EDITOR_TITLE = component {
    text("编辑 <name>")
}

val UI_HOME_EDITOR_RENAME = component {
    text("重命名") with mochaText
}

val UI_HOME_EDITOR_RENAME_LORE = listOf(
    Component.empty(),
    component {
        text("左键 ") with mochaLavender
        text("重命名该家") with mochaText
    }
)

val UI_HOME_EDITOR_MOVE = component {
    text("迁移") with mochaText
}

val UI_HOME_EDITOR_MOVE_LORE = listOf(
    Component.empty(),
    component {
        text("左键 ") with mochaLavender
        text("将该家迁移到你所在的位置") with mochaText
    }
)

val UI_HOME_EDITOR_DELETE = component {
    text("删除") with mochaText
}

val UI_HOME_EDITOR_DELETE_LORE = listOf(
    component { text("该操作不可撤销") with mochaRed },
    Component.empty(),
    component {
        text("Shift + 左键 ") with mochaLavender
        text("删除该家") with mochaText
    }
)

val UI_HOME_EDITOR_SAVED = component {
    text("√ 已保存") with mochaGreen
}

val UI_HOME_EDITOR_SET_PREFER = component {
    text("设为首选") with mochaText
}

val UI_HOME_EDITOR_SET_PREFER_LORE = listOf(
    component { text("设为首选后，") with mochaSubtext0 },
    component {
        text("使用 ") with mochaSubtext0
        text("/home ") with mochaLavender
        text("或「手账」将默认传送该家") with mochaSubtext0
    },
    Component.empty(),
    component {
        text("左键 ") with mochaLavender
        text("将该家设为首选") with mochaText
    }
)

val UI_HOME_EDITOR_UNSET_PREFER = component {
    text("取消首选") with mochaText
}

val UI_HOME_EDITOR_UNSET_PREFER_LORE = listOf(
    Component.empty(),
    component {
        text("左键 ") with mochaLavender
        text("将该家取消首选") with mochaText
    }
)

val UI_HOME_EDITOR_SET_STAR = component {
    text("收藏") with mochaText
}

val UI_HOME_EDITOR_SET_STAR_LORE = listOf(
    component { text("收藏后，该家将靠前显示") with mochaSubtext0 },
    Component.empty(),
    component {
        text("左键 ") with mochaLavender
        text("将该家收藏") with mochaText
    }
)

val UI_HOME_EDITOR_UNSET_STAR = component {
    text("取消收藏") with mochaText
}

val UI_HOME_EDITOR_UNSET_STAR_LORE = listOf(
    Component.empty(),
    component {
        text("左键 ") with mochaLavender
        text("将该家取消收藏") with mochaText
    }
)

val UI_HOME_EDITOR_RENAME_TITLE = component {
    text("重命名 <name>")
}

val UI_HOME_EDITOR_RENAME_EXIT_LORE = listOf(
    Component.empty(),
    component {
        text("左键 ") with mochaLavender
        text("退出编辑") with mochaText
    }
)

fun getUIHomeEditorRenameSaveLore(home: Home): List<Component> {
    val loc = home.location
    return listOf(
        component {
            raw(
                UI_HOME_LIST_HOME_LOCATION
                    .replace("<world>", loc.world.aliasOrName)
                    .replace("<x>", "${loc.blockX}")
                    .replace("<y>", "${loc.blockY}")
                    .replace("<z>", "${loc.blockZ}")
            )
        },
        Component.empty(),
        component {
            text("左键 ") with mochaLavender
            text("保存并退出") with mochaText
        }
    )
}

val UI_HOME_EDITOR_RENAME_SAVE_FAILED_INVALID_LORE = listOf(
    Component.empty(),
    component { text("仅可使用字母、数字、下划线") with mochaMaroon }
)

val UI_HOME_EDITOR_RENAME_SAVE_FAILED_LENGTH_LIMIT_LORE
    get() = listOf(
        Component.empty(),
        component {
            text("名称过长，最多使用 ") with mochaMaroon
            text("${HomeManager.nameLengthLimit} ") with mochaText
            text("个字符") with mochaMaroon
        }
    )

val UI_HOME_EDITOR_RENAME_SAVE_FAILED_EXISTED_LORE = listOf(
    Component.empty(),
    component {
        component { text("已存在同名的家") with mochaMaroon }
    }
)

val UI_HOME_EDITOR_RENAME_SAVED_LORE = listOf(
    Component.empty(),
    UI_HOME_EDITOR_SAVED
)

val UI_HOME_CREATOR_TITLE = component {
    text("创建家")
}

val UI_HOME_CREATOR_EXIT_LORE = listOf(
    Component.empty(),
    component {
        text("左键 ") with mochaLavender
        text("返回上一页") with mochaText
    }
)

const val UI_HOME_CREATOR_INPUT = "输入名称..."

fun getUIHomeCreatorOutputLore(loc: Location): List<Component> {
    return listOf(
        component {
            raw(
                UI_HOME_LIST_HOME_LOCATION
                    .replace("<world>", loc.world.aliasOrName)
                    .replace("<x>", "${loc.blockX}")
                    .replace("<y>", "${loc.blockY}")
                    .replace("<z>", "${loc.blockZ}")
            )
        },
        Component.empty(),
        component {
            text("左键 ") with mochaLavender
            text("创建家") with mochaText
        }
    )
}
