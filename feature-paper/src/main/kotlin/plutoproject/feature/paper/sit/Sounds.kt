package plutoproject.feature.paper.sit

import ink.pmc.advkt.sound.*
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound

val SIT_FAILED_SOUND = sound {
    key(Key.key("block.note_block.hat"))
    source(Sound.Source.BLOCK)
    volume(1f)
    pitch(1f)
}
