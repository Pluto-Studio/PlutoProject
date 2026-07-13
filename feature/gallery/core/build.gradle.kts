plugins {
    id("plutoproject.kotlin-test")
}

dependencies {
    implementation(libs.kotlinx.coroutine.core)
    implementation(libs.hash4j)
    implementation(libs.imageioWebp)

    testImplementation(libs.hash4j)
    testImplementation(libs.imageioWebp)
}
