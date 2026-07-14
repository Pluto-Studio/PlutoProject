plugins {
    id("plutoproject.paper")
    id("plutoproject.runtime-module")
    id("plutoproject.compose")
}

dependencies {
    implementation(projects.kernel.api)
    implementation(projects.kernel.api.paper)
    implementation(projects.foundation.common)
    implementation(projects.capability.interactive.api)
    implementation(libs.adventureKt)
    implementation(libs.bundles.mccoroutine.paper)
    implementation(libs.bundles.voyager)
}
