package plutoproject.feature.gallery.api

import java.util.*

sealed interface GalleryEvent {
    /**
     * 玩家将一幅地图画挂入展示框展出。
     */
    data class ImagePlacement(
        /**
         * 展出的地图画的 [ImageDisplay] 实例。
         *
         * 若展出的地图画无效（可能已被删除）则为空。
         */
        val imageDisplay: ImageDisplay?,

        /**
         * 触发这次事件的玩家。
         */
        val player: UUID,

        /**
         * 用于展出本地图画的展示框实体 ID。
         */
        val itemFrames: List<UUID>,
    ) : GalleryEvent

    /**
     * 玩家将一幅展示框内展出的地图画摘下。
     */
    data class ImageRemoval(
        /**
         * 展出的地图画的 [ImageDisplay] 实例。
         *
         * 若摘下的地图画无效（可能已被删除）则为空。
         */
        val imageDisplay: ImageDisplay?,

        /**
         * 触发这次事件的玩家。
         */
        val player: UUID,

        /**
         * 原本用于展出本地图画的展示框实体 ID。
         */
        val itemFrames: List<UUID>,
    ) : GalleryEvent
}
