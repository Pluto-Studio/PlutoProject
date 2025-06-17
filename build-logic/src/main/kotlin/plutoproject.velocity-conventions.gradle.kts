plugins {
    id("plutoproject.common-conventions")
}

dependencies {
    compileOnly(libs.velocity.api)
    with(extensions.getByType<PlutoDependencyHandlerExtension>()) {
        downloadIfRequired(libs.cloud.velocity)
        downloadIfRequired(libs.bundles.mccoroutine.velocity)
    }
}
