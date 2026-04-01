package plutoproject.feature.gallery.adapter.common

import com.mongodb.kotlin.client.coroutine.MongoCollection
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.QualifierValue
import org.koin.dsl.bind
import org.koin.dsl.module
import plutoproject.feature.gallery.core.MapIdRange
import plutoproject.feature.gallery.core.AllocateMapIdUseCase
import plutoproject.feature.gallery.core.SystemInformationRepository
import plutoproject.feature.gallery.core.decode.decoder.ImageDecoder
import plutoproject.feature.gallery.core.decode.decoder.defaultGifDecoder
import plutoproject.feature.gallery.core.decode.decoder.defaultStaticImageDecoder
import plutoproject.feature.gallery.core.display.DefaultDisplayScheduler
import plutoproject.feature.gallery.core.display.DisplayInstanceRepository
import plutoproject.feature.gallery.core.display.DisplayManager
import plutoproject.feature.gallery.core.display.DisplayScheduler
import plutoproject.feature.gallery.core.display.job.DefaultDisplayJobFactory
import plutoproject.feature.gallery.core.display.job.DefaultSendJobFactory
import plutoproject.feature.gallery.core.display.job.DisplayJobFactory
import plutoproject.feature.gallery.core.display.job.SendJobFactory
import plutoproject.feature.gallery.core.display.usecase.*
import plutoproject.feature.gallery.core.image.ImageDataEntryRepository
import plutoproject.feature.gallery.core.image.ImageManager
import plutoproject.feature.gallery.core.image.ImageRepository
import plutoproject.feature.gallery.core.image.usecase.*
import plutoproject.feature.gallery.core.render.*
import plutoproject.feature.gallery.core.render.mapcolor.AlphaCompositor
import plutoproject.feature.gallery.core.render.mapcolor.MapColorQuantizer
import plutoproject.feature.gallery.core.render.mapcolor.defaultAlphaCompositor
import plutoproject.feature.gallery.core.render.mapcolor.defaultMapColorQuantizer
import plutoproject.feature.gallery.core.render.usecase.RenderAnimatedImageUseCase
import plutoproject.feature.gallery.core.render.usecase.RenderStaticImageUseCase
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

internal object StaticDecoderQualifier : Qualifier {
    override val value: QualifierValue = "gallery.static_decoder"
}

internal object GifDecoderQualifier : Qualifier {
    override val value: QualifierValue = "gallery.gif_decoder"
}

val commonModule = module {
    single<Clock> { Clock.systemUTC() }
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

    single<ImageDecoder>(StaticDecoderQualifier) { defaultStaticImageDecoder(get()) }
    single<ImageDecoder>(GifDecoderQualifier) { defaultGifDecoder(get()) }

    single<FrameSampler> { defaultFrameSampler() }
    single<AlphaCompositor> { defaultAlphaCompositor() }
    single<MapColorQuantizer> { defaultMapColorQuantizer() }

    singleOf(::DefaultStaticImageRenderer) bind StaticImageRenderer::class
    singleOf(::DefaultAnimatedImageRenderer) bind AnimatedImageRenderer::class

    singleOf(::ImageManager)
    singleOf(::DisplayManager)
    singleOf(::DefaultDisplayScheduler) bind DisplayScheduler::class

    single<DisplayJobFactory> {
        val config = get<GalleryConfig>()
        DefaultDisplayJobFactory(
            displayScheduler = get(),
            viewPort = get(),
            displayManager = get(),
            clock = get(),
            animatedMaxFramesPerSecond = config.display.animated.maxFramesPerSecond,
            visibleDistance = config.display.visibleDistance,
            staticUpdateInterval = config.display.static.updateInterval,
        )
    }
    single<SendJobFactory> {
        DefaultSendJobFactory(
            clock = get(),
            coroutineScope = get(),
            loopContext = get(),
            mapUpdatePort = get(),
            maxQueueSize = get<GalleryConfig>().send.maxQueueSize,
            maxUpdatesInSpan = get<GalleryConfig>().send.maxUpdatesInSpan,
            updateLimitSpan = get<GalleryConfig>().send.updateLimitSpan,
        )
    }

    single<DecodeImageUseCase> {
        DecodeImageUseCase(
            pngDecoder = get(StaticDecoderQualifier),
            jpgDecoder = get(StaticDecoderQualifier),
            webpDecoder = get(StaticDecoderQualifier),
            gifDecoder = get(GifDecoderQualifier),
            logger = get(),
        )
    }

    singleOf(::RenderStaticImageUseCase)
    singleOf(::RenderAnimatedImageUseCase)

    singleOf(::CreateImageUseCase)
    single<AllocateMapIdUseCase> {
        val allocationRange = get<GalleryConfig>().mapIdRange
        AllocateMapIdUseCase(
            mapIdRange = MapIdRange(
                start = allocationRange.start,
                end = allocationRange.end,
            ),
            systemInformationRepository = get(),
        )
    }
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
