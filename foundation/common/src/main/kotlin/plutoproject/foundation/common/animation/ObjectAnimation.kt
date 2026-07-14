package plutoproject.foundation.common.animation

interface ObjectAnimation<T> {
    val currentFrame: T
    val frameCount: Int

    fun nextFrame(): T
}
