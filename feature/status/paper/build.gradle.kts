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
    implementation(projects.capability.geoip.api)
    implementation(projects.capability.interactive.api)
    implementation(projects.capability.serverStatistics.api)
    implementation(projects.capability.legacyCloudCommands.api.paper)
    implementation(project(":feature:menu:api:paper"))
    implementation(libs.adventureKt)
    implementation(libs.bundles.hoplite)
    implementation(libs.bundles.koin)
    implementation(libs.bundles.mccoroutine.paper)
    implementation(libs.geoip2)
    implementation(libs.jetbrains.compose.runtime)
}
