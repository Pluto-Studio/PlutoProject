plugins {
    id("plutoproject.kotlin-library")
}

dependencies {
    implementation(libs.kotlinx.serialization.hocon)
    implementation(libs.classgraph)
}
