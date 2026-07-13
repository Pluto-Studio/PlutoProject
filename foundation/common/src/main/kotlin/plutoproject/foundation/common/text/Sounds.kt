package plutoproject.foundation.common.text

import ink.pmc.advkt.sound.key
import ink.pmc.advkt.sound.pitch
import ink.pmc.advkt.sound.sound
import ink.pmc.advkt.sound.source
import ink.pmc.advkt.sound.volume
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound

val MESSAGE_SOUND = sound {
    key(Key.key("block.decorated_pot.insert"))
}

val UI_SUCCEED_SOUND = sound {
    source(Sound.Source.UI)
    key(Key.key("block.note_block.bell"))
}

val UI_FAILED_SOUND = sound {
    source(Sound.Source.UI)
    key(Key.key("block.note_block.didgeridoo"))
}

val UI_PAGING_SOUND = sound {
    source(Sound.Source.UI)
    key(Key.key("item.book.page_turn"))
}

val UI_SELECT_SOUND = sound {
    source(Sound.Source.UI)
    key(Key.key("block.note_block.hat"))
}

val UI_TOGGLE_ON_SOUND = sound {
    source(Sound.Source.UI)
    key(Key.key("ui.button.click"))
    volume(0.4f)
}

val UI_TOGGLE_OFF_SOUND = sound {
    source(Sound.Source.UI)
    key(Key.key("ui.button.click"))
    volume(0.4f)
    pitch(0.75f)
}
