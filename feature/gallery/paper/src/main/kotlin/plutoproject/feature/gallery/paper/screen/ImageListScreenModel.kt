package plutoproject.feature.gallery.paper.screen

import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageStore
import plutoproject.capability.interactive.api.layout.list.ListMenuModel
import plutoproject.kernel.api.koinGet
import java.util.*
import kotlin.math.ceil

private const val PAGE_SIZE = 28

class ImageListScreenModel(private val owner: UUID) : ListMenuModel<Image>() {
    private val imageStore = koinGet<ImageStore>()

    override suspend fun fetchPageContents(): List<Image> {
        val images = imageStore.findByOwner(owner)
            .sortedWith(compareBy<Image> { it.name.lowercase() }.thenBy { it.id })
        pageCount = ceil(images.size.toDouble() / PAGE_SIZE).toInt()
        return images.drop(page * PAGE_SIZE).take(PAGE_SIZE)
    }
}
