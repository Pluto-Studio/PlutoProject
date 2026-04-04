package plutoproject.feature.gallery.core.display

import plutoproject.feature.gallery.core.HORIZONTAL_FOV_RADIAN
import plutoproject.feature.gallery.core.VERTICAL_FOV_RADIAN
import kotlin.math.*

private const val EPSILON = 1e-6

private const val WORLD_UP_X = 0.0
private const val WORLD_UP_Y = 1.0
private const val WORLD_UP_Z = 0.0

private const val FALLBACK_UP_X = 0.0
private const val FALLBACK_UP_Y = 0.0
private const val FALLBACK_UP_Z = 1.0

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
    fun computeVisibleTiles(view: PlayerView, visibleDistance: Double): TileRect? {
        val eye = view.eye
        val forwardLengthSquared = view.viewDirection.lengthSquared()
        if (forwardLengthSquared < 1e-12) return null

        val forwardInvLength = 1.0 / sqrt(forwardLengthSquared)
        val forwardX = view.viewDirection.x * forwardInvLength
        val forwardY = view.viewDirection.y * forwardInvLength
        val forwardZ = view.viewDirection.z * forwardInvLength

        val visibleDist2 = visibleDistance * visibleDistance

        // 先粗筛，早退出避免后续精确计算
        // 这些计算对于单个玩家来说都是 O(1) 的，相对便宜

        // 检查玩家到地图画的距离，超过就直接算看不到
        // distance 实际用的时候默认是 64，因为原版 Misc Entity 的默认追踪距离是 64
        val toCenterX = center.x - eye.x
        val toCenterY = center.y - eye.y
        val toCenterZ = center.z - eye.z
        val dist2ToCenter = lengthSquared(toCenterX, toCenterY, toCenterZ)
        if (dist2ToCenter > visibleDist2) {
            return null
        }

        // 检查玩家是否在地图画正面，客户端地图画渲染是单向的（虽然实际好像也不太可能从背面看到展示框吧...）
        // frontDot > 0 表示 eye 位于 normal 指向的一侧
        val frontDot = dot(
            eye.x - center.x,
            eye.y - center.y,
            eye.z - center.z,
            normal.x,
            normal.y,
            normal.z
        )
        if (frontDot <= 0.0) {
            return null
        }

        val forwardDotCenter = dot(forwardX, forwardY, forwardZ, toCenterX, toCenterY, toCenterZ)
        if (forwardDotCenter <= 0.0) {
            return null
        }

        // 下面是粗筛之后做 4-ray 精确求交

        var rightX = forwardY * WORLD_UP_Z - forwardZ * WORLD_UP_Y
        var rightY = forwardZ * WORLD_UP_X - forwardX * WORLD_UP_Z
        var rightZ = forwardX * WORLD_UP_Y - forwardY * WORLD_UP_X
        if (lengthSquared(rightX, rightY, rightZ) < EPSILON) {
            rightX = forwardY * FALLBACK_UP_Z - forwardZ * FALLBACK_UP_Y
            rightY = forwardZ * FALLBACK_UP_X - forwardX * FALLBACK_UP_Z
            rightZ = forwardX * FALLBACK_UP_Y - forwardY * FALLBACK_UP_X
            if (lengthSquared(rightX, rightY, rightZ) < EPSILON) {
                return null
            }
        }

        val rightLengthSquared = lengthSquared(rightX, rightY, rightZ)
        if (rightLengthSquared < 1e-12) return null
        val rightInvLength = 1.0 / sqrt(rightLengthSquared)
        rightX *= rightInvLength
        rightY *= rightInvLength
        rightZ *= rightInvLength

        var upX = rightY * forwardZ - rightZ * forwardY
        var upY = rightZ * forwardX - rightX * forwardZ
        var upZ = rightX * forwardY - rightY * forwardX
        val upLengthSquared = lengthSquared(upX, upY, upZ)
        if (upLengthSquared < 1e-12) return null
        val upInvLength = 1.0 / sqrt(upLengthSquared)
        upX *= upInvLength
        upY *= upInvLength
        upZ *= upInvLength

        val tanHalfHFov = tan(HORIZONTAL_FOV_RADIAN / 2.0)
        val tanHalfVFov = tan(VERTICAL_FOV_RADIAN / 2.0)

        var minU = Double.POSITIVE_INFINITY
        var maxU = Double.NEGATIVE_INFINITY
        var minV = Double.POSITIVE_INFINITY
        var maxV = Double.NEGATIVE_INFINITY
        var anyHit = false

        fun accumulateRay(rayDirX: Double, rayDirY: Double, rayDirZ: Double): Boolean {
            val hitT = intersectRayWithPlaneT(
                rayOriginX = eye.x,
                rayOriginY = eye.y,
                rayOriginZ = eye.z,
                rayDirX = rayDirX,
                rayDirY = rayDirY,
                rayDirZ = rayDirZ,
                planePointX = center.x,
                planePointY = center.y,
                planePointZ = center.z,
                planeNormalX = normal.x,
                planeNormalY = normal.y,
                planeNormalZ = normal.z
            ) ?: return false

            val hitToEyeX = rayDirX * hitT
            val hitToEyeY = rayDirY * hitT
            val hitToEyeZ = rayDirZ * hitT
            if (lengthSquared(hitToEyeX, hitToEyeY, hitToEyeZ) > visibleDist2) {
                return false
            }

            val localX = eye.x + hitToEyeX - origin.x
            val localY = eye.y + hitToEyeY - origin.y
            val localZ = eye.z + hitToEyeZ - origin.z
            val u = dot(localX, localY, localZ, axisU.x, axisU.y, axisU.z)
            val v = dot(localX, localY, localZ, axisV.x, axisV.y, axisV.z)

            minU = min(minU, u)
            maxU = max(maxU, u)
            minV = min(minV, v)
            maxV = max(maxV, v)
            return true
        }

        anyHit = accumulateRay(
            forwardX + rightX * tanHalfHFov + upX * tanHalfVFov,
            forwardY + rightY * tanHalfHFov + upY * tanHalfVFov,
            forwardZ + rightZ * tanHalfHFov + upZ * tanHalfVFov
        ) || anyHit
        anyHit = accumulateRay(
            forwardX - rightX * tanHalfHFov + upX * tanHalfVFov,
            forwardY - rightY * tanHalfHFov + upY * tanHalfVFov,
            forwardZ - rightZ * tanHalfHFov + upZ * tanHalfVFov
        ) || anyHit
        anyHit = accumulateRay(
            forwardX + rightX * tanHalfHFov - upX * tanHalfVFov,
            forwardY + rightY * tanHalfHFov - upY * tanHalfVFov,
            forwardZ + rightZ * tanHalfHFov - upZ * tanHalfVFov
        ) || anyHit
        anyHit = accumulateRay(
            forwardX - rightX * tanHalfHFov - upX * tanHalfVFov,
            forwardY - rightY * tanHalfHFov - upY * tanHalfVFov,
            forwardZ - rightZ * tanHalfHFov - upZ * tanHalfVFov
        ) || anyHit

        // 如果四个角都没打到，不能直接判 null
        // 可能出现「地图画完全覆盖屏幕中心，但四角射线都偏到展示面外侧」的情况
        // 所以补一条中心射线作为兜底
        val centerHitT = intersectRayWithPlaneT(
            rayOriginX = eye.x,
            rayOriginY = eye.y,
            rayOriginZ = eye.z,
            rayDirX = forwardX,
            rayDirY = forwardY,
            rayDirZ = forwardZ,
            planePointX = center.x,
            planePointY = center.y,
            planePointZ = center.z,
            planeNormalX = normal.x,
            planeNormalY = normal.y,
            planeNormalZ = normal.z
        )

        if (!anyHit) {
            val hitT = centerHitT ?: return null

            val hitToEyeX = forwardX * hitT
            val hitToEyeY = forwardY * hitT
            val hitToEyeZ = forwardZ * hitT
            if (lengthSquared(hitToEyeX, hitToEyeY, hitToEyeZ) > visibleDist2) {
                return null
            }

            val localX = eye.x + hitToEyeX - origin.x
            val localY = eye.y + hitToEyeY - origin.y
            val localZ = eye.z + hitToEyeZ - origin.z
            val u = dot(localX, localY, localZ, axisU.x, axisU.y, axisU.z)
            val v = dot(localX, localY, localZ, axisV.x, axisV.y, axisV.z)

            minU = u
            maxU = u
            minV = v
            maxV = v
        } else {
            // 为了避免「中心在地图画上，但四角全落在外」导致包围范围偏移，
            // 把中心射线命中点也并进 bbox
            if (centerHitT != null) {
                val hitToEyeX = forwardX * centerHitT
                val hitToEyeY = forwardY * centerHitT
                val hitToEyeZ = forwardZ * centerHitT
                if (lengthSquared(hitToEyeX, hitToEyeY, hitToEyeZ) <= visibleDist2) {
                    val localX = eye.x + hitToEyeX - origin.x
                    val localY = eye.y + hitToEyeY - origin.y
                    val localZ = eye.z + hitToEyeZ - origin.z
                    val u = dot(localX, localY, localZ, axisU.x, axisU.y, axisU.z)
                    val v = dot(localX, localY, localZ, axisV.x, axisV.y, axisV.z)

                    minU = min(minU, u)
                    maxU = max(maxU, u)
                    minV = min(minV, v)
                    maxV = max(maxV, v)
                }
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

    private fun intersectRayWithPlaneT(
        rayOriginX: Double,
        rayOriginY: Double,
        rayOriginZ: Double,
        rayDirX: Double,
        rayDirY: Double,
        rayDirZ: Double,
        planePointX: Double,
        planePointY: Double,
        planePointZ: Double,
        planeNormalX: Double,
        planeNormalY: Double,
        planeNormalZ: Double
    ): Double? {
        val denom = dot(rayDirX, rayDirY, rayDirZ, planeNormalX, planeNormalY, planeNormalZ)
        if (abs(denom) < EPSILON) {
            return null
        }

        val t = dot(
            planePointX - rayOriginX,
            planePointY - rayOriginY,
            planePointZ - rayOriginZ,
            planeNormalX,
            planeNormalY,
            planeNormalZ
        ) / denom
        if (t <= 0.0) {
            return null
        }

        return t
    }
}

private fun dot(
    ax: Double,
    ay: Double,
    az: Double,
    bx: Double,
    by: Double,
    bz: Double
): Double = ax * bx + ay * by + az * bz

private fun lengthSquared(x: Double, y: Double, z: Double): Double = x * x + y * y + z * z
