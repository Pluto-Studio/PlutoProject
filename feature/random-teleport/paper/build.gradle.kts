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
    implementation(projects.capability.interactive.api)
    implementation(projects.capability.legacyCloudCommands.api.paper)
    implementation(projects.feature.menu.api.paper)
    implementation(projects.feature.teleport.api.paper)
    implementation(projects.feature.randomTeleport.api.paper)
    implementation(libs.adventureKt)
    implementation(libs.bundles.hoplite)
    implementation(libs.bundles.koin)
    implementation(libs.bundles.cloud)
    implementation(libs.cloud.paper)
    implementation(libs.bundles.mccoroutine.paper)
    implementation(libs.bundles.voyager)
    compileOnly(libs.vaultApi)
}
