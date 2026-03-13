package plutoproject.feature.gallery.adapter.common

import com.mongodb.kotlin.client.coroutine.MongoCollection
import org.koin.core.qualifier.named
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import plutoproject.feature.gallery.core.ImageDataEntryRepository
import plutoproject.feature.gallery.core.ImageManager
import plutoproject.feature.gallery.core.ImageRepository
import plutoproject.feature.gallery.core.decode.decoder.ImageDecoder
import plutoproject.feature.gallery.core.decode.decoder.defaultGifDecoder
import plutoproject.feature.gallery.core.decode.decoder.defaultStaticImageDecoder
import plutoproject.feature.gallery.core.render.AnimatedImageRenderer
import plutoproject.feature.gallery.core.render.DefaultAnimatedImageRenderer
import plutoproject.feature.gallery.core.render.DefaultStaticImageRenderer
import plutoproject.feature.gallery.core.render.StaticImageRenderer
import plutoproject.feature.gallery.core.render.defaultFrameSampler
import plutoproject.feature.gallery.core.render.mapcolor.defaultAlphaCompositor
import plutoproject.feature.gallery.core.render.mapcolor.defaultMapColorQuantizer
import plutoproject.feature.gallery.core.usecase.ChangeImageOwnerNameUseCase
import plutoproject.feature.gallery.core.usecase.CreateImageDataEntryUseCase
import plutoproject.feature.gallery.core.usecase.CreateImageUseCase
import plutoproject.feature.gallery.core.usecase.DeleteImageDataEntryUseCase
import plutoproject.feature.gallery.core.usecase.DeleteImageUseCase
import plutoproject.feature.gallery.core.usecase.DecodeImageUseCase
import plutoproject.feature.gallery.core.usecase.GetImageDataEntryUseCase
import plutoproject.feature.gallery.core.usecase.GetImageUseCase
import plutoproject.feature.gallery.core.usecase.LookupImageByOwnerUseCase
import plutoproject.feature.gallery.core.usecase.RenameImageUseCase
import plutoproject.feature.gallery.core.usecase.RenderAnimatedImageUseCase
import plutoproject.feature.gallery.core.usecase.RenderStaticImageUseCase
import plutoproject.feature.gallery.core.usecase.ReplaceImageDataEntryUseCase
import plutoproject.feature.gallery.infra.mongo.MongoImageDataEntryRepository
import plutoproject.feature.gallery.infra.mongo.MongoImageRepository
import plutoproject.feature.gallery.infra.mongo.model.ImageDataEntryDocument
import plutoproject.feature.gallery.infra.mongo.model.ImageDocument
import plutoproject.framework.common.api.connection.MongoConnection
import plutoproject.framework.common.api.connection.getCollection
import java.time.Clock

private const val GALLERY_PREFIX = "gallery_"
private const val IMAGE_COLLECTION = "images"
private const val IMAGE_DATA_ENTRY_COLLECTION = "image_data_entries"

private inline fun <reified T : Any> getCollection(name: String): MongoCollection<T> {
    return MongoConnection.getCollection("$GALLERY_PREFIX$name")
}

val commonModule = module {
    singleOf(::ImageManager)

    single<ImageRepository> {
        MongoImageRepository(getCollection<ImageDocument>(IMAGE_COLLECTION))
    }
    single<ImageDataEntryRepository> {
        MongoImageDataEntryRepository(getCollection<ImageDataEntryDocument>(IMAGE_DATA_ENTRY_COLLECTION))
    }

    single<ImageDecoder>(named("gallery_static_decoder")) { defaultStaticImageDecoder() }
    single<ImageDecoder>(named("gallery_gif_decoder")) { defaultGifDecoder() }

    single { defaultFrameSampler() }
    single { defaultAlphaCompositor() }
    single { defaultMapColorQuantizer() }

    single<StaticImageRenderer> {
        DefaultStaticImageRenderer(
            alphaCompositor = get(),
            mapColorQuantizer = get(),
            logger = get(named("gallery_logger")),
        )
    }
    single<AnimatedImageRenderer> {
        DefaultAnimatedImageRenderer(
            frameSampler = get(),
            alphaCompositor = get(),
            mapColorQuantizer = get(),
            logger = get(named("gallery_logger")),
        )
    }

    single {
        DecodeImageUseCase(
            pngDecoder = get(named("gallery_static_decoder")),
            jpgDecoder = get(named("gallery_static_decoder")),
            webpDecoder = get(named("gallery_static_decoder")),
            gifDecoder = get(named("gallery_gif_decoder")),
            logger = get(named("gallery_logger")),
        )
    }
    singleOf(::RenderStaticImageUseCase)
    singleOf(::RenderAnimatedImageUseCase)

    singleOf(::CreateImageUseCase)
    singleOf(::GetImageUseCase)
    singleOf(::DeleteImageUseCase)
    singleOf(::RenameImageUseCase)
    singleOf(::ChangeImageOwnerNameUseCase)
    singleOf(::LookupImageByOwnerUseCase)

    singleOf(::CreateImageDataEntryUseCase)
    singleOf(::GetImageDataEntryUseCase)
    singleOf(::DeleteImageDataEntryUseCase)
    singleOf(::ReplaceImageDataEntryUseCase)
}
