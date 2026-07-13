plugins {
    id("plutoproject.paper")
    id("plutoproject.runtime-module")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
    implementation(projects.kernel.api)
    implementation(projects.kernel.api.paper)
    implementation(projects.foundation.common)
    implementation(projects.foundation.paper)
    implementation(projects.capability.interactive.api)
    implementation(projects.capability.worldAlias.api)
    implementation(projects.capability.legacyCloudCommands.api.paper)
    implementation(projects.feature.menu.api.paper)
    implementation(projects.feature.afk.api.paper)
    implementation(projects.feature.teleport.api.paper)
    implementation(libs.adventureKt)
    implementation(libs.bundles.hoplite)
    implementation(libs.bundles.koin)
    implementation(libs.bundles.cloud)
    implementation(libs.bundles.mccoroutine.paper)
    implementation(libs.jetbrains.compose.runtime)
    implementation(libs.jetbrains.compose.runtime.saveable)
    implementation(libs.bundles.voyager)
}
