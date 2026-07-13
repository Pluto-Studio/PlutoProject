package plutoproject.feature.teleport.paper

import ink.pmc.advkt.sound.key
import ink.pmc.advkt.sound.sound
import net.kyori.adventure.key.Key

val TELEPORT_PREPARING_SOUND = sound {
    key(Key.key("block.amethyst_cluster.hit"))
}

val TELEPORT_SUCCEED_SOUND = sound {
    key(Key.key("entity.enderman.teleport"))
}
