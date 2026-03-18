package plutoproject.feature.gallery.adapter.common

import com.mongodb.kotlin.client.coroutine.MongoCollection
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import plutoproject.feature.gallery.core.AllocateMapIdUseCase
import plutoproject.feature.gallery.core.SystemInformationRepository
import plutoproject.feature.gallery.core.decode.decoder.ImageDecoder
import plutoproject.feature.gallery.core.decode.decoder.defaultGifDecoder
import plutoproject.feature.gallery.core.decode.decoder.defaultStaticImageDecoder
import plutoproject.feature.gallery.core.display.job.DefaultDisplayJobFactory
import plutoproject.feature.gallery.core.display.DefaultDisplayScheduler
import plutoproject.feature.gallery.core.display.job.DefaultSendJobFactory
import plutoproject.feature.gallery.core.display.DisplayInstanceRepository
import plutoproject.feature.gallery.core.display.job.DisplayJobFactory
import plutoproject.feature.gallery.core.display.DisplayManager
import plutoproject.feature.gallery.core.display.DisplayScheduler
import plutoproject.feature.gallery.core.display.job.SendJobFactory
import plutoproject.feature.gallery.core.display.usecase.AttachDisplayInstanceToJobUseCase
import plutoproject.feature.gallery.core.display.usecase.CreateDisplayInstanceUseCase
import plutoproject.feature.gallery.core.display.usecase.DeleteDisplayInstanceUseCase
import plutoproject.feature.gallery.core.display.usecase.DetachDisplayInstanceFromJobUseCase
import plutoproject.feature.gallery.core.display.usecase.GetDisplayInstanceUseCase
import plutoproject.feature.gallery.core.display.usecase.GetDisplayInstancesByIdsUseCase
import plutoproject.feature.gallery.core.display.usecase.LookupDisplayInstanceByBelongsUseCase
import plutoproject.feature.gallery.core.display.usecase.LookupDisplayInstanceByChunkUseCase
import plutoproject.feature.gallery.core.display.usecase.StartDisplayJobUseCase
import plutoproject.feature.gallery.core.display.usecase.StartSendJobUseCase
import plutoproject.feature.gallery.core.display.usecase.StopDisplayJobUseCase
import plutoproject.feature.gallery.core.display.usecase.StopSendJobUseCase
import plutoproject.feature.gallery.core.image.ImageDataEntryRepository
import plutoproject.feature.gallery.core.image.ImageManager
import plutoproject.feature.gallery.core.image.ImageRepository
import plutoproject.feature.gallery.core.image.usecase.ChangeImageOwnerNameUseCase
import plutoproject.feature.gallery.core.image.usecase.CreateImageDataEntryUseCase
import plutoproject.feature.gallery.core.image.usecase.CreateImageUseCase
import plutoproject.feature.gallery.core.image.usecase.DecodeImageUseCase
import plutoproject.feature.gallery.core.image.usecase.DeleteImageDataEntryUseCase
import plutoproject.feature.gallery.core.image.usecase.DeleteImageUseCase
import plutoproject.feature.gallery.core.image.usecase.GetImageDataEntriesByBelongsToUseCase
import plutoproject.feature.gallery.core.image.usecase.GetImageDataEntryUseCase
import plutoproject.feature.gallery.core.image.usecase.GetImageUseCase
import plutoproject.feature.gallery.core.image.usecase.GetImagesByIdsUseCase
import plutoproject.feature.gallery.core.image.usecase.LookupImageByOwnerUseCase
import plutoproject.feature.gallery.core.image.usecase.RenameImageUseCase
import plutoproject.feature.gallery.core.render.usecase.RenderAnimatedImageUseCase
import plutoproject.feature.gallery.core.render.usecase.RenderStaticImageUseCase
import plutoproject.feature.gallery.core.image.usecase.ReplaceImageDataEntryUseCase
import plutoproject.feature.gallery.core.render.AnimatedImageRenderer
import plutoproject.feature.gallery.core.render.DefaultAnimatedImageRenderer
import plutoproject.feature.gallery.core.render.DefaultStaticImageRenderer
import plutoproject.feature.gallery.core.render.StaticImageRenderer
import plutoproject.feature.gallery.core.render.defaultFrameSampler
import plutoproject.feature.gallery.core.render.mapcolor.defaultAlphaCompositor
import plutoproject.feature.gallery.core.render.mapcolor.defaultMapColorQuantizer
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
import java.time.Clock

private const val GALLERY_PREFIX = "gallery_"
private const val IMAGE_COLLECTION = "image"
private const val IMAGE_DATA_ENTRY_COLLECTION = "image_data"
private const val DISPLAY_INSTANCE_COLLECTION = "display_instance"
private const val SYSTEM_INFORMATION_COLLECTION = "system_information"

private inline fun <reified T : Any> getCollection(name: String): MongoCollection<T> {
    return MongoConnection.getCollection("$GALLERY_PREFIX${serverName}_$name")
}

val commonModule = module {
    single<Clock>(named("gallery_clock")) { Clock.systemUTC() }
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

    single<DisplayScheduler> {
        DefaultDisplayScheduler(
            clock = get(named("gallery_clock")),
            coroutineScope = get(named("gallery_coroutine_scope")),
            schedulerContext = get(named("gallery_scheduler_context")),
            awakeContext = get(named("gallery_awake_context"))
        )
    }
    single<DisplayJobFactory> {
        DefaultDisplayJobFactory(
            displayScheduler = get(),
            viewPort = get(),
            displayManager = get(),
            clock = get(named("gallery_clock")),
        )
    }
    single<SendJobFactory> {
        DefaultSendJobFactory(
            clock = get(named("gallery_clock")),
            coroutineScope = get(named("gallery_coroutine_scope")),
            loopContext = get(named("gallery_awake_context")),
            mapUpdatePort = get(),
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
    singleOf(::AllocateMapIdUseCase)
    singleOf(::GetImageUseCase)
    singleOf(::DeleteImageUseCase)
    singleOf(::RenameImageUseCase)
    singleOf(::ChangeImageOwnerNameUseCase)
    singleOf(::LookupImageByOwnerUseCase)

    singleOf(::CreateDisplayInstanceUseCase)
    singleOf(::DeleteDisplayInstanceUseCase)
    singleOf(::GetDisplayInstanceUseCase)
    singleOf(::GetDisplayInstancesByIdsUseCase)
    singleOf(::LookupDisplayInstanceByBelongsUseCase)
    singleOf(::LookupDisplayInstanceByChunkUseCase)

    singleOf(::CreateImageDataEntryUseCase)
    singleOf(::GetImageDataEntryUseCase)
    singleOf(::GetImageDataEntriesByBelongsToUseCase)
    singleOf(::DeleteImageDataEntryUseCase)
    singleOf(::ReplaceImageDataEntryUseCase)

    singleOf(::GetImagesByIdsUseCase)

    singleOf(::StartDisplayJobUseCase)
    singleOf(::StopDisplayJobUseCase)
    singleOf(::AttachDisplayInstanceToJobUseCase)
    singleOf(::DetachDisplayInstanceFromJobUseCase)
    singleOf(::StartSendJobUseCase)
    singleOf(::StopSendJobUseCase)
}
