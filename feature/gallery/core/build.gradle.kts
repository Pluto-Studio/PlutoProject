plugins {
    id("plutoproject.kotlin-test")
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutine.core)
    compileOnly(libs.hash4j)
    compileOnly(libs.imageioWebp)

    testImplementation(libs.hash4j)
    testImplementation(libs.imageioWebp)
}
