package plutoproject.feature.gallery.common

import org.koin.core.Koin
import plutoproject.feature.gallery.common.upload.UploadService
import plutoproject.feature.gallery.common.upload.startWebServer
import plutoproject.feature.gallery.common.upload.stopWebServer
import plutoproject.feature.gallery.core.display.DisplayRuntimeRegistry
import plutoproject.feature.gallery.core.display.DisplayScheduler
import plutoproject.feature.gallery.core.display.job.SendJobRegistry
import java.util.logging.Logger

fun onFeatureEnable(koin: Koin, logger: Logger) {
    startWebServer(koin.get(), koin.get(), logger)
}

suspend fun onFeatureDisable(koin: Koin) {
    stopWebServer()
    koin.get<UploadService>().close()
    koin.get<DisplayScheduler>().stop()
    koin.get<DisplayRuntimeRegistry>().close()
    koin.get<SendJobRegistry>().close()
}
