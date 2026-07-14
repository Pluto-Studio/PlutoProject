plugins {
    id("plutoproject.paper-devbundle")
    id("plutoproject.runtime-module")
    id("plutoproject.compose")
}

dependencies {
    implementation(projects.kernel.api)
    implementation(projects.kernel.api.paper)
    implementation(projects.foundation.common)
    implementation(projects.foundation.paper)
    implementation(projects.capability.mongo.api)
    implementation(projects.capability.serverIdentifier.api)
    implementation(projects.capability.interactive.api)
    implementation(projects.capability.legacyCloudCommands.api.paper)
    implementation(projects.feature.gallery.api)
    implementation(projects.feature.gallery.core)
    implementation(projects.feature.gallery.mongo)
    implementation(projects.feature.gallery.common)
    implementation(projects.feature.menu.api.paper)
    implementation(libs.adventureKt)
    implementation(libs.bundles.hoplite)
    implementation(libs.bundles.koin)
    implementation(libs.bundles.mccoroutine.paper)
    implementation(libs.bundles.voyager)
    implementation(libs.okhttp)
}
