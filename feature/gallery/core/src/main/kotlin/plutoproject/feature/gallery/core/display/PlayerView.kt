package plutoproject.feature.gallery.core.display

import java.util.UUID

data class PlayerView(
    /**
     * 玩家 UUID。
     */
    val id: UUID,

    /**
     * 玩家眼睛位置。
     */
    val eye: Vec3,

    /**
     * 玩家视线，需要是单位向量。
     */
    val viewDirection: Vec3
)
