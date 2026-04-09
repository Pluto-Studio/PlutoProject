package plutoproject.feature.gallery.adapter.common

import plutoproject.feature.gallery.core.display.DisplayRuntimeRegistry
import plutoproject.feature.gallery.core.display.DisplayScheduler
import plutoproject.feature.gallery.core.display.job.SendJobRegistry

fun onFeatureEnable() {
    startWebServer()
}

fun onFeatureDisable() {
    koin.get<DisplayScheduler>().stop()
    koin.get<DisplayRuntimeRegistry>().close()
    koin.get<SendJobRegistry>().close()
    stopWebServer()
}
