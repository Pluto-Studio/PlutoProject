plugins {
    id("plutoproject.core")
}

dependencies {
    implementation(projects.foundation.common)
    api(libs.cloud.annotations)
    compileOnly(libs.velocity.api)
}
