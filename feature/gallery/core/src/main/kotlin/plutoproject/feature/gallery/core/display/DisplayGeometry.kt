package plutoproject.feature.gallery.core.display

import kotlin.math.*

data class DisplayGeometry(
    /**
     * 第一个展示框（Tile ID 为 0，地图画左上角）的中心位置。
     */
    val origin: Vec3,

    /**
     * 整个地图画的中心位置。
     */
    val center: Vec3,

    /**
     * 地图画 Tile 坐标 (X, Y) 中，X 增长的方向，需要是单位向量。
     */
    val axisU: Vec3,

    /**
     * 地图画 Tile 坐标 (X, Y) 中，Y 增长的方向，需要是单位向量。
     */
    val axisV: Vec3,

    /**
     * 地图的朝向（法线）向量。
     */
    val normal: Vec3,

    /**
     * 地图画宽方块数。
     */
    val widthBlocks: Int,

    /**
     * 地图画长方块数。
     */
    val heightBlocks: Int,
) {
    fun computeVisibleTiles(
        playerViews: List<PlayerView>,
        visibleDistance: Double,
        horizontalFovRadian: Double,
        verticalFovRadian: Double
    ): Map<PlayerView, TileRect> {
        val result = HashMap<PlayerView, TileRect>(playerViews.size)

        for (view in playerViews) {
            val rect = computeVisibleTilesForView(
                view = view,
                visibleDistance = visibleDistance,
                horizontalFovRadian = horizontalFovRadian,
                verticalFovRadian = verticalFovRadian
            )
            if (rect != null) {
                result[view] = rect
            }
        }

        return result
    }

    private fun computeVisibleTilesForView(
        view: PlayerView,
        visibleDistance: Double,
        horizontalFovRadian: Double,
        verticalFovRadian: Double
    ): TileRect? {
        val eye = view.eye
        val forward = view.viewDirection.normalizedOrNull() ?: return null

        val visibleDist2 = visibleDistance * visibleDistance

        // 先粗筛，早退出避免后续精确计算
        // 这些计算对于单个玩家来说都是 O(1) 的，相对便宜

        // 检查玩家到地图画的距离，超过就直接算看不到
        // distance 实际用的时候默认是 64，因为原版 Misc Entity 的默认追踪距离是 64
        val toCenter = center - eye
        val dist2ToCenter = toCenter.lengthSquared()
        if (dist2ToCenter > visibleDist2) {
            return null
        }

        // 检查玩家是否在地图画正面，客户端地图画渲染是单向的（虽然实际好像也不太可能从背面看到展示框吧...）
        // frontDot > 0 表示 eye 位于 normal 指向的一侧
        val frontDot = (eye - center).dot(normal)
        if (frontDot <= 0.0) {
            return null
        }

        val forwardDotCenter = forward.dot(toCenter)
        if (forwardDotCenter <= 0.0) {
            return null
        }

        // 下面是粗筛之后做 4-ray 精确求交

        val cameraBasis = buildCameraBasis(forward) ?: return null
        val right = cameraBasis.right
        val up = cameraBasis.up

        val tanHalfHFov = tan(horizontalFovRadian / 2.0)
        val tanHalfVFov = tan(verticalFovRadian / 2.0)

        // 屏幕四角对应的四条边界射线方向
        // 不要求单位长度，因为平面求交中的 t 参数会自动吸收长度比例
        val rayDirs = arrayOf(
            (forward + right * tanHalfHFov + up * tanHalfVFov).normalizedOrNull(),
            (forward - right * tanHalfHFov + up * tanHalfVFov).normalizedOrNull(),
            (forward + right * tanHalfHFov - up * tanHalfVFov).normalizedOrNull(),
            (forward - right * tanHalfHFov - up * tanHalfVFov).normalizedOrNull()
        )

        var minU = Double.POSITIVE_INFINITY
        var maxU = Double.NEGATIVE_INFINITY
        var minV = Double.POSITIVE_INFINITY
        var maxV = Double.NEGATIVE_INFINITY
        var anyHit = false

        val planePoint = center

        for (dir in rayDirs) {
            val rayDir = dir ?: continue

            val hit = intersectRayWithPlane(
                rayOrigin = eye,
                rayDir = rayDir,
                planePoint = planePoint,
                planeNormal = normal
            ) ?: continue

            // 只接受可见距离内的点
            if ((hit - eye).lengthSquared() > visibleDist2) {
                continue
            }

            // 投到 tile 局部坐标
            val local = hit - origin
            val u = local.dot(axisU)
            val v = local.dot(axisV)

            minU = min(minU, u)
            maxU = max(maxU, u)
            minV = min(minV, v)
            maxV = max(maxV, v)
            anyHit = true
        }

        // 如果四个角都没打到，不能直接判 null
        // 可能出现「地图画完全覆盖屏幕中心，但四角射线都偏到展示面外侧」的情况
        // 所以补一条中心射线作为兜底
        if (!anyHit) {
            val centerHit = intersectRayWithPlane(
                rayOrigin = eye,
                rayDir = forward,
                planePoint = planePoint,
                planeNormal = normal
            ) ?: return null

            if ((centerHit - eye).lengthSquared() > visibleDist2) {
                return null
            }

            val local = centerHit - origin
            val u = local.dot(axisU)
            val v = local.dot(axisV)

            minU = u
            maxU = u
            minV = v
            maxV = v
        } else {
            // 为了避免「中心在地图画上，但四角全落在外」导致包围范围偏移，
            // 把中心射线命中点也并进 bbox
            val centerHit = intersectRayWithPlane(
                rayOrigin = eye,
                rayDir = forward,
                planePoint = planePoint,
                planeNormal = normal
            )

            if (centerHit != null && (centerHit - eye).lengthSquared() <= visibleDist2) {
                val local = centerHit - origin
                val u = local.dot(axisU)
                val v = local.dot(axisV)

                minU = min(minU, u)
                maxU = max(maxU, u)
                minV = min(minV, v)
                maxV = max(maxV, v)
            }
        }

        // 把上一步算出来的包围空间转换成 Tile Rect
        // 这里算的偏保守，宁可多发也不要少发导致看起来有问题

        var minTileX = ceil(minU - 0.5).toInt()
        var maxTileX = floor(maxU + 0.5).toInt()
        var minTileY = ceil(minV - 0.5).toInt()
        var maxTileY = floor(maxV + 0.5).toInt()

        // 浮点边界附近做个兜底修正，避免极端情况下出现反转
        if (maxTileX < minTileX) maxTileX = minTileX
        if (maxTileY < minTileY) maxTileY = minTileY

        // clamp 到地图画边界
        if (minTileX < 0) minTileX = 0
        if (minTileY < 0) minTileY = 0
        if (maxTileX >= widthBlocks) maxTileX = widthBlocks - 1
        if (maxTileY >= heightBlocks) maxTileY = heightBlocks - 1

        if (minTileX > maxTileX || minTileY > maxTileY) {
            return null
        }

        return TileRect(
            minX = minTileX,
            maxX = maxTileX,
            minY = minTileY,
            maxY = maxTileY
        )
    }

    private fun buildCameraBasis(forward: Vec3): CameraBasis? {
        // 优先用世界 Y 轴作为 up 参考
        val worldUp = Vec3(0.0, 1.0, 0.0)

        var right = forward.cross(worldUp)
        if (right.lengthSquared() < EPSILON) {
            // 说明 forward 与 worldUp 近似平行，换一个参考轴
            val fallbackUp = Vec3(0.0, 0.0, 1.0)
            right = forward.cross(fallbackUp)
            if (right.lengthSquared() < EPSILON) {
                return null
            }
        }

        right = right.normalizedOrNull() ?: return null
        val up = right.cross(forward).normalizedOrNull() ?: return null

        return CameraBasis(right = right, up = up)
    }

    private fun intersectRayWithPlane(
        rayOrigin: Vec3,
        rayDir: Vec3,
        planePoint: Vec3,
        planeNormal: Vec3
    ): Vec3? {
        val denom = rayDir.dot(planeNormal)
        if (abs(denom) < EPSILON) {
            return null
        }

        val t = (planePoint - rayOrigin).dot(planeNormal) / denom
        if (t <= 0.0) {
            return null
        }

        return rayOrigin + rayDir * t
    }

    private data class CameraBasis(
        val right: Vec3,
        val up: Vec3
    )

    private companion object {
        private const val EPSILON = 1e-6
    }
}
