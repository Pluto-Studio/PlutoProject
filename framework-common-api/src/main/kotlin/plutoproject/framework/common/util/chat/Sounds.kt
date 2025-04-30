package plutoproject.framework.common.util.chat

import ink.pmc.advkt.sound.key
import ink.pmc.advkt.sound.sound
import net.kyori.adventure.key.Key

val MESSAGE_SOUND = sound {
    key(Key.key("block.decorated_pot.insert"))
}

val UI_INVALID_SOUND = sound {
    key(Key.key("block.note_block.didgeridoo"))
}

val UI_SUCCEED_SOUND = sound {
    key(Key.key("block.note_block.bell"))
}

val UI_PAGING_SOUND = sound {
    key(Key.key("item.book.page_turn"))
}

val UI_SELECTOR_SOUND = sound {
    key(Key.key("block.note_block.hat"))
}
