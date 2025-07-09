plugins {
    id("plutoproject.velocity-conventions")
    id("plutoproject.dokka-conventions")
}

dependencies {
    api(projects.frameworkVelocityApi)
    api(projects.featureCommonApi)
}
