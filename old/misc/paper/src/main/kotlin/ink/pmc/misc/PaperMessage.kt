package ink.pmc.misc

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.keybind
import ink.pmc.advkt.component.text
import ink.pmc.advkt.sound.*
import ink.pmc.advkt.title.*
import ink.pmc.framework.chat.*
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.title.Title
import kotlin.time.Duration.Companion.seconds

val SUICIDE = component {
    text("你结束了自己的生命...") with mochaFlamingo
}

@Suppress("UNUSED")
val STAND_UP = component {
    text("使用 ") with mochaPink
    keybind("key.sneak") with mochaText
    text(" 键起身") with mochaPink
}

val ILLEGAL_LOC = component {
    text("无法在此处坐下，请检查是否有实体方块以及足够的空间") with mochaMaroon
}

val MULTI_SITTERS_TITLE = title {
    mainTitle {
        text(" ")
    }
    subTitle {
        text("此位置已有其他人坐下") with mochaMaroon
    }
    times {
        fadeIn(0.seconds)
        stay(1.seconds)
        fadeOut(0.seconds)
    }
}

val MULTI_SITTERS_SOUND = sound {
    key(Key.key("block.note_block.hat"))
    source(Sound.Source.BLOCK)
    volume(1f)
    pitch(1f)
}

val CHAT_FORMAT = component {
    text("<player>") with mochaFlamingo
    text(": ") with mochaSubtext0
    text("<message>") with mochaText
}

val JOIN_FORMAT = component {
    text("[+] ") with mochaGreen
    text("<player> ") with mochaFlamingo
    text("加入了游戏") with mochaPink
}

val QUIT_FORMAT = component {
    text("[-] ") with mochaRed
    text("<player> ") with mochaFlamingo
    text("退出了游戏") with mochaPink
}

fun elevatorGoUpTitle(curr: Int, total: Int): Title {
    return title {
        mainTitle {
            text(" ")
        }
        subTitle {
            text("电梯上行 ") with mochaYellow
            text("($curr/$total)") with mochaSubtext0
        }
        times {
            fadeIn(0.seconds)
            stay(1.seconds)
            fadeOut(0.seconds)
        }
    }
}

fun elevatorGoDownTitle(curr: Int, total: Int): Title {
    return title {
        mainTitle {
            text(" ")
        }
        subTitle {
            text("电梯下行 ") with mochaYellow
            text("($curr/$total)") with mochaSubtext0
        }
        times {
            fadeIn(0.seconds)
            stay(1.seconds)
            fadeOut(0.seconds)
        }
    }
}

val ELEVATOR_WORK_SOUND = sound {
    key(Key.key("entity.iron_golem.attack"))
    source(Sound.Source.BLOCK)
    volume(1f)
    pitch(1f)
}

val LEATHER_SIT_SOUND = sound {
    key(Key.key("item.armor.equip_leather"))
    source(Sound.Source.BLOCK)
}

val CHAINMAIL_SIT_SOUND = sound {
    key(Key.key("item.armor.equip_chain"))
    source(Sound.Source.BLOCK)
}

val IRON_SIT_SOUND = sound {
    key(Key.key("item.armor.equip_iron"))
    source(Sound.Source.BLOCK)
}

val GOLD_SIT_SOUND = sound {
    key(Key.key("item.armor.equip_gold"))
    source(Sound.Source.BLOCK)
}

val DIAMOND_SIT_SOUND = sound {
    key(Key.key("item.armor.equip_diamond"))
    source(Sound.Source.BLOCK)
}

val NETHERITE_SIT_SOUND = sound {
    key(Key.key("item.armor.equip_netherite"))
    source(Sound.Source.BLOCK)
}

val GENERIC_SIT_SOUND = sound {
    key(Key.key("item.armor.equip_generic"))
    source(Sound.Source.BLOCK)
}