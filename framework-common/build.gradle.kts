plugins {
    id("plutoproject.common-conventions")
}

dependencies {
    api(projects.frameworkCommonApi)
    compileOnly(libs.ksp.api)
}
