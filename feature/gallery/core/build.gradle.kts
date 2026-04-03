plugins {
    id("plutoproject.test-conventions")
}

dependencies {
    compileOnly(libs.hash4j)
    compileOnly(libs.imageioWebp)

    testImplementation(libs.hash4j)
    testImplementation(libs.imageioWebp)
}
