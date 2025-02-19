plugins {
    id("plutoproject.common-conventions")
}

dependencies {
    compileOnly(libs.velocity)
    with(extensions.getByType<PlutoDependencyHandlerExtension>()) {
        downloadIfRequired(libs.cloud.velocity)
        downloadIfRequired(libs.packetevents.velocity)
        downloadIfRequired(libs.bundles.mccoroutine.velocity)
    }
}
