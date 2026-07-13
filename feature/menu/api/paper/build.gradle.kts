plugins {
    id("plutoproject.paper")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
    implementation(projects.kernel.api)
    implementation(projects.capability.interactive.api)
    implementation(libs.jetbrains.compose.runtime)
}
