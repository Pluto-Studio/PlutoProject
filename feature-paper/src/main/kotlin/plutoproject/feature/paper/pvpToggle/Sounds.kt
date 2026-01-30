package plutoproject.feature.paper.pvpToggle

import ink.pmc.advkt.sound.*
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound

val PVP_DISABLED_SOUND = sound {
    key(Key.key("block.note_block.hat"))
    source(Sound.Source.PLAYER)
    volume(1f)
    pitch(1f)
}
