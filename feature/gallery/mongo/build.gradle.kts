plugins {
    id("plutoproject.kotlin-test")
}

dependencies {
    implementation(projects.feature.gallery.core)
    implementation(projects.foundation.common)
    implementation(libs.bundles.mongodb)
    implementation(libs.zstdJni)

    testImplementation(projects.foundation.common)
    testImplementation(libs.bundles.mongodb)
    testImplementation(libs.testcontainers.mongodb)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.zstdJni)
}
