package plutoproject.feature.gallery.core.image

import plutoproject.feature.gallery.core.image.TilePool

/**
 * 预渲染的静态图数据。
 */
class StaticImageData(
    /**
     * 所有可以复用的预渲染 Tile 数据。
     */
    val tilePool: TilePool,

    /**
     * 存放 [tilePool] index 矩阵的展平表示。
     *
     * - 每个 index 为 U16，存储于 [Short] 中，使用时需要转为无符号语义：`(v.toInt() and 0xFFFF)`
     * - tile 顺序：从左到右、从上到下（与 [Image.tileMapIds] 的顺序一致）
     *
     * 设 `tileCount = mapXBlocks * mapYBlocks`，
     * 则数组长度恒为 `tileCount`。
     *
     * 读取方式：
     *
     * ```kotlin
     * val tileIndex = y * mapXBlocks + x
     * val tilePoolIndex = tileIndexes[tileIndex]
     * ```
     */
    val tileIndexes: ShortArray,
)
