package plutoproject.framework.common.util.animation

interface ObjectAnimation<T> {
    val currentFrame: T
    val frameCount: Int

    fun nextFrame(): T
}
