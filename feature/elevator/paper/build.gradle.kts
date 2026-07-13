plugins {
    id("plutoproject.paper")
    id("plutoproject.runtime-module")
}

dependencies {
    implementation(projects.kernel.api)
    implementation(projects.kernel.api.paper)
    implementation(projects.foundation.common)
    implementation(projects.foundation.paper)
    implementation(projects.feature.elevator.api.paper)
    implementation(libs.bundles.koin)
    implementation(libs.bundles.mccoroutine.paper)
}
