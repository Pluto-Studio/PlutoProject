package plutoproject.feature.gallery.adapter.paper.screen

import plutoproject.feature.gallery.adapter.common.koin
import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageStore
import plutoproject.framework.paper.api.interactive.layout.list.ListMenuModel
import java.util.*
import kotlin.math.ceil

private const val PAGE_SIZE = 28

class ImageListScreenModel(private val owner: UUID) : ListMenuModel<Image>() {
    private val imageStore by koin.inject<ImageStore>()

    override suspend fun fetchPageContents(): List<Image> {
        val images = imageStore.findByOwner(owner)
            .sortedWith(compareBy<Image> { it.name.lowercase() }.thenBy { it.id })
        pageCount = ceil(images.size.toDouble() / PAGE_SIZE).toInt()
        return images.drop(page * PAGE_SIZE).take(PAGE_SIZE)
    }
}
