package plutoproject.feature.gallery.core.display.job

import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageDataEntry
import plutoproject.feature.gallery.core.display.DisplayInstance
import java.util.UUID

interface DisplayJob {
    val belongsTo: UUID

    val isStopped: Boolean

    val managedDisplayInstances: Map<UUID, DisplayInstance>

    /**
     * 接管一个 [DisplayInstance]。
     *
     * [DisplayJob] 会直接持有传入的共享对象引用，不会 clone / snapshot。
     * 若当前 job 已经处于 stopped 终态，必须抛出异常。
     */
    fun attach(
        displayInstance: DisplayInstance,
        image: Image,
        imageDataEntry: ImageDataEntry<*>,
    )

    /**
     * 解除接管一个 [DisplayInstance]。
     *
     * stopped 后允许 no-op。
     */
    fun detach(displayInstanceId: UUID): DisplayInstance?

    fun isEmpty(): Boolean

    fun wake()

    fun stop()
}
