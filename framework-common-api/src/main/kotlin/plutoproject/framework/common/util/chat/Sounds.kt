package plutoproject.framework.common.util.chat

import ink.pmc.advkt.sound.*
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

val GENERIC_ACTION_DENIED_SOUND = sound {
    key(Key.key("block.note_block.hat"))
    volume(1f)
    pitch(1f)
}
