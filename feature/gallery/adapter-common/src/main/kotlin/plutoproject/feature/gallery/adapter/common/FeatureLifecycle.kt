package plutoproject.feature.gallery.adapter.common

import kotlinx.coroutines.runBlocking
import plutoproject.feature.gallery.adapter.common.upload.UploadService
import plutoproject.feature.gallery.adapter.common.upload.startWebServer
import plutoproject.feature.gallery.adapter.common.upload.stopWebServer
import plutoproject.feature.gallery.core.display.DisplayRuntimeRegistry
import plutoproject.feature.gallery.core.display.DisplayScheduler
import plutoproject.feature.gallery.core.display.job.SendJobRegistry

fun onFeatureEnable() {
    startWebServer()
}

fun onFeatureDisable(): Unit = runBlocking {
    stopWebServer()
    koin.get<UploadService>().close()
    koin.get<DisplayScheduler>().stop()
    koin.get<DisplayRuntimeRegistry>().close()
    koin.get<SendJobRegistry>().close()
}
