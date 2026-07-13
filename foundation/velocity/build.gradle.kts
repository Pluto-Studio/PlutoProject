plugins {
    id("plutoproject.kotlin-library")
}

dependencies {
    implementation(projects.foundation.common)
    api(libs.cloud.annotations)
    compileOnly(libs.velocity.api)
}
