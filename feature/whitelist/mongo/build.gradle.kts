plugins {
    id("plutoproject.test-conventions")
}

dependencies {
    implementation(projects.foundation.common)
    implementation(projects.feature.whitelist.core)
    implementation(libs.bundles.mongodb)

    testImplementation(projects.foundation.common)
    testImplementation(libs.bundles.mongodb)
    testImplementation(libs.testcontainers.mongodb)
}
