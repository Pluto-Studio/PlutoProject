package plutoproject.feature.gallery.core.render

/**
 * 预渲染流程的状态码。
 *
 * 约定：
 * - 返回 [SUCCEED] 时必须携带非空的 `imageData`
 * - 返回非 [SUCCEED] 时必须返回 `imageData = null`
 */
enum class RenderStatus {
    /** 渲染成功。 */
    SUCCEED,

    /**
     * 由 `mapXBlocks * mapYBlocks` 推导得到的 tile 数量不合法。
     *
     * 典型原因：`mapXBlocks <= 0` 或 `mapYBlocks <= 0`。
     *
     * 通常由 UseCase 在调用 renderer 前返回。
     */
    INVALID_TILE_COUNT,

    /**
     * 由 `mapXBlocks * mapYBlocks` 推导得到的 tile 数量溢出。
     *
     * 典型原因：tile 数量超过 `Int.MAX_VALUE`，导致无法分配 `ShortArray`/`ByteArray` 等索引矩阵。
     *
     * 通常由 UseCase 在调用 renderer 前返回。
     */
    TILE_COUNT_OVERFLOW,

    /**
     * 动图输入源帧数量不合法。
     *
     * 典型原因：`sourceFrames.isEmpty()`。
     *
     * 通常由 UseCase 在调用 renderer 前返回。
     */
    INVALID_SOURCE_FRAME_COUNT,

    /**
     * 动图渲染结果的 `frameCount` 不合法。
     *
     * 典型原因：renderer 返回 `frameCount <= 0`。
     *
     * 通常由 UseCase 的结果校验返回。
     */
    INVALID_RENDERED_FRAME_COUNT,

    /**
     * 动图渲染结果的 `durationMillis` 不合法。
     *
     * 典型原因：renderer 返回 `durationMillis <= 0`。
     *
     * 通常由 UseCase 的结果校验返回。
     */
    INVALID_RENDERED_DURATION_MILLIS,

    /**
     * 期望的 `tileIndexes` 长度溢出。
     *
     * 典型原因：`singleFrameTileCount * frameCount > Int.MAX_VALUE`。
     *
     * 通常由 UseCase 的结果校验返回。
     */
    TILE_INDEXES_LENGTH_OVERFLOW,

    /**
     * `tileIndexes` 长度与期望不一致。
     *
     * 典型原因：
     * - 静态图：`tileIndexes.size != mapXBlocks * mapYBlocks`
     * - 动图：`tileIndexes.size != (mapXBlocks * mapYBlocks) * frameCount`
     *
     * 通常由 UseCase 的结果校验返回。
     */
    TILE_INDEXES_LENGTH_MISMATCH,

    /**
     * unique tile 数量超过 TilePool 的上限（U16 索引）。
     *
     * 上限为 `65536`。
     *
     * 该状态可能由 dedup 阶段直接返回，也可能由 UseCase 的结果校验返回。
     */
    UNIQUE_TILE_OVERFLOW,

    /**
     * renderer 返回了自相矛盾的结果（例如 status=SUCCEED 但 imageData 为 null）。
     *
     * 通常由 UseCase 的结果校验返回。
     */
    INCONSISTENT_RENDER_RESULT,

    /**
     * pipeline 执行失败，但失败原因未细分为其他状态码。
     *
     * 典型原因：内部异常、暂未支持的输入/算法组合等。
     */
    PIPELINE_FAILED,
}

/**
 * 预渲染结果。
 *
 * - [status] 为 [RenderStatus.SUCCEED] 时，期望 [imageData] 非空
 * - [status] 失败时，期望 [imageData] 为空
 */
data class RenderResult<T>(
    val status: RenderStatus,
    val imageData: T?,
) {
    companion object {
        fun <T> succeed(imageData: T): RenderResult<T> = RenderResult(
            status = RenderStatus.SUCCEED,
            imageData = imageData,
        )

        fun <T> failed(status: RenderStatus): RenderResult<T> {
            require(status != RenderStatus.SUCCEED) {
                "failed status cannot be SUCCEED"
            }
            return RenderResult(status = status, imageData = null)
        }
    }
}
