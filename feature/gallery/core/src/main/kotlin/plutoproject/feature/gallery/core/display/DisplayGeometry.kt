package plutoproject.feature.gallery.core.display

import kotlin.math.*

private const val EPSILON = 1e-6

private const val WORLD_UP_X = 0.0
private const val WORLD_UP_Y = 1.0
private const val WORLD_UP_Z = 0.0

private const val FALLBACK_UP_X = 0.0
private const val FALLBACK_UP_Y = 0.0
private const val FALLBACK_UP_Z = 1.0

data class DisplayGeometry(
    val origin: Vec3,
    val center: Vec3,
    val axisU: Vec3,
    val axisV: Vec3,
    val normal: Vec3,
    val widthBlocks: Int,
    val heightBlocks: Int,
) {
    fun computeVisibleTiles(
        view: PlayerView,
        visibleDistance: Double,
        horizontalFovRadian: Double = Math.PI / 2.0,
        verticalFovRadian: Double = Math.PI / 2.0
    ): TileRect? {
        require(widthBlocks > 0) { "widthBlocks must be > 0" }
        require(heightBlocks > 0) { "heightBlocks must be > 0" }
        require(horizontalFovRadian > 0.0 && horizontalFovRadian < Math.PI) {
            "horizontalFovRadian must be in (0, PI)"
        }
        require(verticalFovRadian > 0.0 && verticalFovRadian < Math.PI) {
            "verticalFovRadian must be in (0, PI)"
        }

        val eye = view.eye

        // Step 1:
        // 规范化玩家 forward，构建相机基底 forward / right / up
        val forward = view.viewDirection.normalizedOrNull() ?: return null
        val right = computeCameraRight(forward) ?: return null
        val up = right.cross(forward).normalizedOrNull() ?: return null

        // Step 2:
        // 极粗筛：先看“玩家到展示矩形最近可能距离”是否超过 visibleDistance
        // 这里不用 center distance，避免大画误杀角落可见情况。
        val displayBoundingRadius = hypot(widthBlocks / 2.0, heightBlocks / 2.0)
        val distToCenter = eye.distanceTo(center)
        if (distToCenter - displayBoundingRadius > visibleDistance) {
            return null
        }

        // Step 3:
        // 单面展示：如果玩家在背面，直接不可见。
        // eye 位于 center + normal 指向的一侧时，dot(eye - center, normal) > 0
        val frontDot = (eye - center).dot(normal)
        if (frontDot <= 0.0) {
            return null
        }

        // Step 4:
        // 构造展示矩形 4 个角点。
        // origin 是 tile(0,0) 的中心，因此整个展示矩形边界在：
        // u in [-0.5, widthBlocks - 0.5]
        // v in [-0.5, heightBlocks - 0.5]
        val corners = listOf(
            pointAtUv(-0.5, -0.5),
            pointAtUv(widthBlocks - 0.5, -0.5),
            pointAtUv(widthBlocks - 0.5, heightBlocks - 0.5),
            pointAtUv(-0.5, heightBlocks - 0.5),
        )

        // Step 5:
        // 构造视锥半空间平面。
        //
        // 我们不直接写平面方程 ax+by+cz+d=0，而是统一用：
        // inside(p) <=> dot(p - eye, planeNormal) >= 0
        //
        // 所以 planeNormal 必须指向“视锥内部”。
        val halfHFov = horizontalFovRadian / 2.0
        val halfVFov = verticalFovRadian / 2.0

        val leftPlaneNormal = (forward * sin(halfHFov) + right * cos(halfHFov)).normalizedOrNull() ?: return null
        val rightPlaneNormal = (forward * sin(halfHFov) - right * cos(halfHFov)).normalizedOrNull() ?: return null
        val topPlaneNormal = (forward * sin(halfVFov) - up * cos(halfVFov)).normalizedOrNull() ?: return null
        val bottomPlaneNormal = (forward * sin(halfVFov) + up * cos(halfVFov)).normalizedOrNull() ?: return null

        // 前方半空间。只保留 dot(p - eye, forward) >= 0 的点。
        val nearPlaneNormal = forward

        // Step 6:
        // 依次把展示矩形裁到这些半空间里。
        var polygon = corners
        polygon = clipPolygonAgainstPlane(polygon, eye, nearPlaneNormal)
        if (polygon.isEmpty()) return null

        polygon = clipPolygonAgainstPlane(polygon, eye, leftPlaneNormal)
        if (polygon.isEmpty()) return null

        polygon = clipPolygonAgainstPlane(polygon, eye, rightPlaneNormal)
        if (polygon.isEmpty()) return null

        polygon = clipPolygonAgainstPlane(polygon, eye, topPlaneNormal)
        if (polygon.isEmpty()) return null

        polygon = clipPolygonAgainstPlane(polygon, eye, bottomPlaneNormal)
        if (polygon.isEmpty()) return null

        // Step 7:
        // 再做一次距离裁剪。这里只做“点距离”保守筛即可。
        // 更严格可以加一个球面/平面裁剪，但工程上通常不值。
        polygon = clipPolygonByDistance(polygon, eye, visibleDistance)
        if (polygon.isEmpty()) return null

        // Step 8:
        // 把裁剪结果投影到展示画的 UV 坐标。
        var minU = Double.POSITIVE_INFINITY
        var maxU = Double.NEGATIVE_INFINITY
        var minV = Double.POSITIVE_INFINITY
        var maxV = Double.NEGATIVE_INFINITY

        for (p in polygon) {
            val local = p - origin
            val u = local.dot(axisU)
            val v = local.dot(axisV)

            minU = min(minU, u)
            maxU = max(maxU, u)
            minV = min(minV, v)
            maxV = max(maxV, v)
        }

        // Step 9:
        // 防守式 clamp 到展示矩形边界。
        minU = max(minU, -0.5)
        maxU = min(maxU, widthBlocks - 0.5)
        minV = max(minV, -0.5)
        maxV = min(maxV, heightBlocks - 0.5)

        if (minU > maxU || minV > maxV) {
            return null
        }

        // Step 10:
        // UV 区间转 tile 区间。
        //
        // 第 i 列 tile 占据 [i - 0.5, i + 0.5]
        // 所以与 [minU, maxU] 相交的 tile index 范围是：
        // minX = ceil(minU - 0.5)
        // maxX = floor(maxU + 0.5)
        var minTileX = ceil(minU - 0.5).toInt()
        var maxTileX = floor(maxU + 0.5).toInt()
        var minTileY = ceil(minV - 0.5).toInt()
        var maxTileY = floor(maxV + 0.5).toInt()

        if (maxTileX < minTileX) maxTileX = minTileX
        if (maxTileY < minTileY) maxTileY = minTileY

        minTileX = minTileX.coerceIn(0, widthBlocks - 1)
        maxTileX = maxTileX.coerceIn(0, widthBlocks - 1)
        minTileY = minTileY.coerceIn(0, heightBlocks - 1)
        maxTileY = maxTileY.coerceIn(0, heightBlocks - 1)

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

    /**
     * 根据展示画局部 UV，计算世界坐标点。
     */
    private fun pointAtUv(u: Double, v: Double): Vec3 {
        return origin + axisU * u + axisV * v
    }
}

/**
 * 用一个过 eye、法向量为 planeNormal 的平面裁剪多边形。
 *
 * inside 条件：
 * dot(p - planePoint, planeNormal) >= 0
 */
private fun clipPolygonAgainstPlane(
    polygon: List<Vec3>,
    planePoint: Vec3,
    planeNormal: Vec3
): List<Vec3> {
    if (polygon.isEmpty()) return emptyList()

    val result = ArrayList<Vec3>(polygon.size + 2)

    for (i in polygon.indices) {
        val current = polygon[i]
        val next = polygon[(i + 1) % polygon.size]

        val currentDist = (current - planePoint).dot(planeNormal)
        val nextDist = (next - planePoint).dot(planeNormal)

        val currentInside = currentDist >= -EPSILON
        val nextInside = nextDist >= -EPSILON

        when {
            currentInside && nextInside -> {
                // inside -> inside：保留 next
                result += next
            }

            currentInside && !nextInside -> {
                // inside -> outside：保留交点
                val intersection = intersectSegmentWithPlane(
                    current, next, planePoint, planeNormal, currentDist, nextDist
                )
                if (intersection != null) {
                    result += intersection
                }
            }

            !currentInside && nextInside -> {
                // outside -> inside：保留交点 + next
                val intersection = intersectSegmentWithPlane(
                    current, next, planePoint, planeNormal, currentDist, nextDist
                )
                if (intersection != null) {
                    result += intersection
                }
                result += next
            }

            else -> {
                // outside -> outside：什么都不保留
            }
        }
    }

    return deduplicateAdjacentPoints(result)
}

/**
 * 用距离球做保守裁剪。
 *
 * 这里不是严格球裁剪，而是：
 * - 保留球内点
 * - 若线段穿过球边界，加入交点
 *
 * 这一步是为了避免多边形顶点全在视锥内，但超过可见距离。
 */
private fun clipPolygonByDistance(
    polygon: List<Vec3>,
    eye: Vec3,
    maxDistance: Double
): List<Vec3> {
    if (polygon.isEmpty()) return emptyList()

    val r2 = maxDistance * maxDistance
    val result = ArrayList<Vec3>(polygon.size + 2)

    for (i in polygon.indices) {
        val current = polygon[i]
        val next = polygon[(i + 1) % polygon.size]

        val currentInside = (current - eye).lengthSquared() <= r2 + EPSILON
        val nextInside = (next - eye).lengthSquared() <= r2 + EPSILON

        when {
            currentInside && nextInside -> {
                result += next
            }

            currentInside && !nextInside -> {
                val intersection = intersectSegmentWithSphere(current, next, eye, maxDistance, keepNearCurrent = true)
                if (intersection != null) result += intersection
            }

            !currentInside && nextInside -> {
                val intersection = intersectSegmentWithSphere(current, next, eye, maxDistance, keepNearCurrent = false)
                if (intersection != null) result += intersection
                result += next
            }

            else -> {
                val hits = intersectSegmentWithSphereBoth(current, next, eye, maxDistance)
                if (hits.size == 2) {
                    // outside -> outside 但穿球而过，加入进入点和离开点
                    result += hits[0]
                    result += hits[1]
                }
            }
        }
    }

    return deduplicateAdjacentPoints(result)
}

private fun intersectSegmentWithPlane(
    a: Vec3,
    b: Vec3,
    planePoint: Vec3,
    planeNormal: Vec3,
    aSignedDistance: Double = (a - planePoint).dot(planeNormal),
    bSignedDistance: Double = (b - planePoint).dot(planeNormal)
): Vec3? {
    val denom = aSignedDistance - bSignedDistance
    if (abs(denom) < EPSILON) return null

    val t = aSignedDistance / denom
    if (t < -EPSILON || t > 1.0 + EPSILON) return null

    return a.lerpTo(b, t.coerceIn(0.0, 1.0))
}

private fun intersectSegmentWithSphere(
    a: Vec3,
    b: Vec3,
    center: Vec3,
    radius: Double,
    keepNearCurrent: Boolean
): Vec3? {
    val hits = intersectSegmentWithSphereBoth(a, b, center, radius)
    if (hits.isEmpty()) return null
    return if (hits.size == 1) {
        hits[0]
    } else {
        if (keepNearCurrent) hits.minByOrNull { (it - a).lengthSquared() }
        else hits.minByOrNull { (it - b).lengthSquared() }
    }
}

private fun intersectSegmentWithSphereBoth(
    a: Vec3,
    b: Vec3,
    center: Vec3,
    radius: Double
): List<Vec3> {
    val d = b - a
    val f = a - center

    val aa = d.dot(d)
    val bb = 2.0 * f.dot(d)
    val cc = f.dot(f) - radius * radius

    val discriminant = bb * bb - 4.0 * aa * cc
    if (discriminant < -EPSILON || aa < EPSILON) {
        return emptyList()
    }

    val disc = max(0.0, discriminant)
    val sqrtDisc = sqrt(disc)

    val t1 = (-bb - sqrtDisc) / (2.0 * aa)
    val t2 = (-bb + sqrtDisc) / (2.0 * aa)

    val hits = mutableListOf<Pair<Double, Vec3>>()

    if (t1 in -EPSILON..(1.0 + EPSILON)) {
        hits += t1.coerceIn(0.0, 1.0) to a.lerpTo(b, t1.coerceIn(0.0, 1.0))
    }
    if (t2 in -EPSILON..(1.0 + EPSILON) && abs(t2 - t1) > EPSILON) {
        hits += t2.coerceIn(0.0, 1.0) to a.lerpTo(b, t2.coerceIn(0.0, 1.0))
    }

    return hits.sortedBy { it.first }.map { it.second }
}

private fun computeCameraRight(forward: Vec3): Vec3? {
    var right = forward.cross(Vec3(WORLD_UP_X, WORLD_UP_Y, WORLD_UP_Z))
    if (right.lengthSquared() < EPSILON) {
        right = forward.cross(Vec3(FALLBACK_UP_X, FALLBACK_UP_Y, FALLBACK_UP_Z))
        if (right.lengthSquared() < EPSILON) {
            return null
        }
    }
    return right.normalizedOrNull()
}

private fun deduplicateAdjacentPoints(points: List<Vec3>): List<Vec3> {
    if (points.isEmpty()) return emptyList()

    val result = ArrayList<Vec3>(points.size)
    for (p in points) {
        if (result.isEmpty() || (p - result.last()).lengthSquared() > EPSILON * EPSILON) {
            result += p
        }
    }

    if (result.size >= 2 && (result.first() - result.last()).lengthSquared() <= EPSILON * EPSILON) {
        result.removeAt(result.lastIndex)
    }

    return result
}
