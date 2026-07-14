plugins {
    id("plutoproject.paper-devbundle")
    id("plutoproject.compose")
}

dependencies {
    implementation(projects.foundation.common)
    implementation(libs.adventureKt)
    implementation(libs.bundles.voyager)
}
