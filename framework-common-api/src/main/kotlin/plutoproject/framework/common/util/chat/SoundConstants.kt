package plutoproject.framework.common.util.chat

import ink.pmc.advkt.sound.key
import ink.pmc.advkt.sound.sound
import net.kyori.adventure.key.Key

object SoundConstants {
    val message = sound {
        key(Key.key("block.decorated_pot.insert"))
    }

    object UI {
        val invalid = sound {
            key(Key.key("block.note_block.didgeridoo"))
        }
        val succeed = sound {
            key(Key.key("block.note_block.bell"))
        }
        val paging = sound {
            key(Key.key("item.book.page_turn"))
        }
        val selector = sound {
            key(Key.key("block.note_block.hat"))
        }
    }
}
