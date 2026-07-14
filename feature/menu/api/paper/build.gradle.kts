plugins {
    id("plutoproject.paper")
    id("plutoproject.compose")
}

dependencies {
    implementation(projects.kernel.api)
    implementation(projects.capability.interactive.api)
}
