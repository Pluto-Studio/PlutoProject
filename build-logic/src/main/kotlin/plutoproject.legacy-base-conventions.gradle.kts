plugins {
    id("plutoproject.core-conventions")
    kotlin("kapt")
    id("com.google.devtools.ksp")
}

val dependencyExtension =
    dependencies.extensions.create<PlutoDependencyHandlerExtension>(
        "plutoDependency",
        project,
    )

tasks.findByName("kspKotlin")?.apply {
    outputs.cacheIf { false }
}

dependencies {
    with(dependencyExtension) {
        downloadIfRequired(libs.bundles.language)
        downloadIfRequired(libs.bundles.mongodb)
        downloadIfRequired(libs.bundles.koin)
        downloadIfRequired(libs.bundles.hoplite)
        downloadIfRequired(libs.bundles.commons)
        downloadIfRequired(libs.okhttp)
        downloadIfRequired(libs.gson)
        downloadIfRequired(libs.caffeine)
        downloadIfRequired(libs.catppuccin)
        downloadIfRequired(libs.classgraph)
        downloadIfRequired(libs.geoip2)
        downloadIfRequired(libs.aedile)
        downloadIfRequired(libs.guava)
        downloadIfRequired(libs.okio)
        downloadIfRequired(libs.charonflow)
    }
}
