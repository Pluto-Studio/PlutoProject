plugins {
    id("plutoproject.paper")
    id("plutoproject.runtime-module")
}

dependencies {
    implementation(projects.kernel.api)
    implementation(projects.kernel.api.paper)
    implementation(projects.foundation.common)
    implementation(projects.capability.interactive.api)
    implementation(libs.adventureKt)
    implementation(libs.bundles.mccoroutine.paper)
    implementation(libs.jetbrains.compose.runtime)
    implementation(libs.bundles.voyager)
}
