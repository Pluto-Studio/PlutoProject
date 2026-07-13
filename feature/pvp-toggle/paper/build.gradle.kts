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
    implementation(projects.capability.databasePersist.api)
    implementation(projects.capability.interactive.api)
    implementation(projects.feature.menu.api.paper)
    implementation(projects.feature.pvpToggle.api.paper)
    implementation(libs.bundles.koin)
    implementation(libs.bundles.mccoroutine.paper)
    implementation(libs.jetbrains.compose.runtime)
}
