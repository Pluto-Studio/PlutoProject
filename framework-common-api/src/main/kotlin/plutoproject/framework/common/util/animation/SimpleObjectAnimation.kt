package plutoproject.framework.common.util.animation

abstract class SimpleObjectAnimation<T> : ObjectAnimation<T> {
    abstract val frames: Array<T>

    private var frameIndex = 0
    override val currentFrame: T
        get() = frames[frameIndex]
    override val frameCount: Int
        get() = frames.size

    override fun nextFrame(): T {
        if (frameIndex < frames.size - 1) {
            frameIndex++
        } else {
            frameIndex = 0
        }
        return currentFrame
    }
}
