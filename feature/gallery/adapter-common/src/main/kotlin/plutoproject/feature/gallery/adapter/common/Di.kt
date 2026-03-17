package plutoproject.feature.gallery.adapter.common

import com.mongodb.kotlin.client.coroutine.MongoCollection
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import plutoproject.feature.gallery.core.DefaultDisplayScheduler
import plutoproject.feature.gallery.core.DisplayInstanceRepository
import plutoproject.feature.gallery.core.DisplayManager
import plutoproject.feature.gallery.core.ImageDataEntryRepository
import plutoproject.feature.gallery.core.ImageManager
import plutoproject.feature.gallery.core.ImageRepository
import plutoproject.feature.gallery.core.SystemInformationRepository
import plutoproject.feature.gallery.core.decode.decoder.ImageDecoder
import plutoproject.feature.gallery.core.decode.decoder.defaultGifDecoder
import plutoproject.feature.gallery.core.decode.decoder.defaultStaticImageDecoder
import plutoproject.feature.gallery.core.render.*
import plutoproject.feature.gallery.core.render.mapcolor.defaultAlphaCompositor
import plutoproject.feature.gallery.core.render.mapcolor.defaultMapColorQuantizer
import plutoproject.feature.gallery.core.usecase.*
import plutoproject.feature.gallery.infra.mongo.MongoDisplayInstanceRepository
import plutoproject.feature.gallery.infra.mongo.MongoImageDataEntryRepository
import plutoproject.feature.gallery.infra.mongo.MongoImageRepository
import plutoproject.feature.gallery.infra.mongo.MongoSystemInformationRepository
import plutoproject.feature.gallery.infra.mongo.model.DisplayInstanceDocument
import plutoproject.feature.gallery.infra.mongo.model.ImageDataEntryDocument
import plutoproject.feature.gallery.infra.mongo.model.ImageDocument
import plutoproject.feature.gallery.infra.mongo.model.MapIdSystemInformationDocument
import plutoproject.framework.common.api.connection.MongoConnection
import plutoproject.framework.common.api.connection.getCollection
import plutoproject.framework.common.util.serverName

private const val GALLERY_PREFIX = "gallery_"
private const val IMAGE_COLLECTION = "image"
private const val IMAGE_DATA_ENTRY_COLLECTION = "image_data"
private const val DISPLAY_INSTANCE_COLLECTION = "display_instance"
private const val SYSTEM_INFORMATION_COLLECTION = "system_information"

private inline fun <reified T : Any> getCollection(name: String): MongoCollection<T> {
    return MongoConnection.getCollection("$GALLERY_PREFIX${serverName}_$name")
}

val commonModule = module {
    single<ImageRepository> {
        MongoImageRepository(getCollection<ImageDocument>(IMAGE_COLLECTION))
    }
    single<DisplayInstanceRepository> {
        MongoDisplayInstanceRepository(getCollection<DisplayInstanceDocument>(DISPLAY_INSTANCE_COLLECTION))
    }
    single<ImageDataEntryRepository> {
        MongoImageDataEntryRepository(getCollection<ImageDataEntryDocument>(IMAGE_DATA_ENTRY_COLLECTION))
    }
    single<SystemInformationRepository> {
        MongoSystemInformationRepository(
            getCollection<MapIdSystemInformationDocument>(SYSTEM_INFORMATION_COLLECTION)
        )
    }

    single<ImageDecoder>(named("gallery_static_decoder")) { defaultStaticImageDecoder(logger = get(named("gallery_logger"))) }
    single<ImageDecoder>(named("gallery_gif_decoder")) { defaultGifDecoder(logger = get(named("gallery_logger"))) }

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

    singleOf(::ImageManager)
    singleOf(::DisplayManager)
    singleOf(::DefaultDisplayScheduler)

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
    singleOf(::AllocateMapIdUseCase)
    singleOf(::GetImageUseCase)
    singleOf(::DeleteImageUseCase)
    singleOf(::RenameImageUseCase)
    singleOf(::ChangeImageOwnerNameUseCase)
    singleOf(::LookupImageByOwnerUseCase)

    singleOf(::CreateDisplayInstanceUseCase)
    singleOf(::DeleteDisplayInstanceUseCase)
    singleOf(::GetDisplayInstanceUseCase)
    singleOf(::LookupDisplayInstanceByBelongsUseCase)
    singleOf(::LookupDisplayInstanceByChunkUseCase)

    singleOf(::CreateImageDataEntryUseCase)
    singleOf(::GetImageDataEntryUseCase)
    singleOf(::DeleteImageDataEntryUseCase)
    singleOf(::ReplaceImageDataEntryUseCase)
}
