plugins {
    id("plutoproject.core")
}

dependencies {
    implementation(libs.kotlinx.serialization.hocon)
    implementation(libs.classgraph)
}
