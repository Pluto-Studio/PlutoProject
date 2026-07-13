plugins {
    id("plutoproject.paper")
}

dependencies {
    implementation(projects.foundation.common)
    implementation(libs.bundles.mccoroutine.paper)
    api(libs.cloud.annotations)
}
