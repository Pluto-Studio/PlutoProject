plugins {
    id("plutoproject.paper")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
    implementation(projects.foundation.common)
    implementation(libs.adventureKt)
    implementation(libs.jetbrains.compose.runtime)
    implementation(libs.jetbrains.compose.runtime.saveable)
    implementation(libs.bundles.voyager)
}
