plugins {
    id("plutoproject.base-conventions")
}

dependencies {
    api(libs.gremlin.runtime)
    compileOnly(libs.luckperms.api)
    with(extensions.getByType<PlutoDependencyHandlerExtension>()) {
        downloadIfRequired(libs.adventureKt)
        downloadIfRequired(libs.bundles.cloud)
    }
}
