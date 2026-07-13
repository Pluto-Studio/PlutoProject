plugins {
    id("plutoproject.common-conventions")
}

dependencies {
    implementation(projects.feature.gallery.api)
    implementation(projects.feature.gallery.mongo)
    implementation(projects.feature.gallery.frontend)
}
