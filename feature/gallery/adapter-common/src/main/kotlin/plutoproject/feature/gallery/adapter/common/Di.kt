package plutoproject.feature.gallery.adapter.common

import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import plutoproject.feature.gallery.core.AllocateMapIdUseCase
import plutoproject.feature.gallery.core.MapIdRange
import plutoproject.feature.gallery.core.SystemInformationRepository
import plutoproject.feature.gallery.core.display.DisplayInstanceRepository
import plutoproject.feature.gallery.core.display.DisplayInstanceStore
import plutoproject.feature.gallery.core.display.DisplayRuntimeRegistry
import plutoproject.feature.gallery.core.display.DisplayScheduler
import plutoproject.feature.gallery.core.display.job.DisplayJobFactory
import plutoproject.feature.gallery.core.display.job.DisplayResourceFactory
import plutoproject.feature.gallery.core.display.job.SendJobFactory
import plutoproject.feature.gallery.core.display.job.SendJobRegistry
import plutoproject.feature.gallery.core.image.ImageDataRepository
import plutoproject.feature.gallery.core.image.ImageDataStore
import plutoproject.feature.gallery.core.image.ImageRepository
import plutoproject.feature.gallery.core.image.ImageStore
import plutoproject.feature.gallery.infra.mongo.MongoDisplayInstanceRepository
import plutoproject.feature.gallery.infra.mongo.MongoImageDataRepository
import plutoproject.feature.gallery.infra.mongo.MongoImageRepository
import plutoproject.feature.gallery.infra.mongo.MongoSystemInformationRepository
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
    single<Clock> { Clock.systemUTC() }
    single<ImageRepository> {
        MongoImageRepository(getCollection(IMAGE_COLLECTION)).also { repo ->
            get<CoroutineScope>().launch(Dispatchers.IO) {
                repo.ensureIndexes()
            }
        }
    }
    single<DisplayInstanceRepository> {
        MongoDisplayInstanceRepository(getCollection(DISPLAY_INSTANCE_COLLECTION)).also { repo ->
            get<CoroutineScope>().launch(Dispatchers.IO) {
                repo.ensureIndexes()
            }
        }
    }
    single<ImageDataRepository> {
        MongoImageDataRepository(getCollection(IMAGE_DATA_ENTRY_COLLECTION)).also { repo ->
            get<CoroutineScope>().launch(Dispatchers.IO) {
                repo.ensureIndexes()
            }
        }
    }
    single<SystemInformationRepository> {
        MongoSystemInformationRepository(
            getCollection(SYSTEM_INFORMATION_COLLECTION)
        )
    }

    singleOf(::ImageStore)
    singleOf(::ImageDataStore)
    singleOf(::DisplayInstanceStore)
    singleOf(::DisplayScheduler)
    singleOf(::DisplayResourceFactory)
    singleOf(::SendJobRegistry)
    singleOf(::DisplayRuntimeRegistry)

    single {
        val config = get<GalleryConfig>()
        DisplayJobFactory(
            displayScheduler = get(),
            viewPort = get(),
            sendJobRegistry = get(),
            clock = get(),
            animatedMaxFramesPerSecond = config.display.animated.maxFramesPerSecond,
            visibleDistance = config.display.visibleDistance,
            staticUpdateInterval = config.display.static.updateInterval,
        )
    }
    single {
        SendJobFactory(
            clock = get(),
            coroutineScope = get(),
            loopContext = get(),
            mapUpdatePort = get(),
            maxQueueSize = get<GalleryConfig>().send.maxQueueSize,
            maxUpdatesInSpan = get<GalleryConfig>().send.maxUpdatesInSpan,
            updateLimitSpan = get<GalleryConfig>().send.updateLimitSpan,
        )
    }
    single<AllocateMapIdUseCase> {
        val allocationRange = get<GalleryConfig>().image.mapIdRange
        AllocateMapIdUseCase(
            mapIdRange = MapIdRange(
                start = allocationRange.start,
                end = allocationRange.end,
            ),
            systemInformationRepository = get(),
        )
    }
}
