plugins {
    id("plutoproject.core")
}

dependencies {
    implementation(projects.foundation.common)
    implementation(projects.feature.gallery.api)
    implementation(projects.feature.gallery.core)
    implementation(projects.feature.gallery.mongo)
    implementation(projects.feature.gallery.frontend)
    implementation(projects.capability.mongo.api)
    implementation(projects.capability.serverIdentifier.api)
    implementation(libs.bundles.koin)
    implementation(libs.bundles.ktor.server)
}
