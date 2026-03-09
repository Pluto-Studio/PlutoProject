package plutoproject.feature.gallery.core

/**
 * 预渲染的动图数据。
 */
class AnimatedImageData(
    /**
     * 动图总帧数。
     */
    val frameCount: Int,

    /**
     * 动图总时长，以毫秒为单位。
     */
    val durationMillis: Int,

    /**
     * 所有可以复用的预渲染 Tile 数据。
     */
    val tilePool: TilePool,

    /**
     * 存放所有帧的 [tilePool] index 矩阵的展平表示。
     *
     * - 每个 index 为 U16，存储于 [Short] 中，使用时需要转为无符号语义：`(v.toInt() and 0xFFFF)`
     * - tile 顺序：从左到右、从上到下（与 [Image.tileMapIds] 的顺序一致）
     * - 帧顺序：从第 0 帧到第 (frameCount-1) 帧依次拼接
     *
     * 设 `singleFrameTileCount = mapXBlocks * mapYBlocks`，
     * 则数组长度恒为 `frameCount * singleFrameTileCount`。
     *
     * 读取方式：
     *
     * ```kotlin
     * val base = frameIndex * singleFrameTileCount
     * val tileIndexInFrame = y * mapXBlocks + x
     * val tilePoolIndex = tileIndexes[base + tileIndexInFrame]
     * ```
     */
    val tileIndexes: ShortArray,
)
