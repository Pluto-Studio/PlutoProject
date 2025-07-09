plugins {
    id("plutoproject.paper-conventions")
    id("plutoproject.dokka-conventions")
}

dependencies {
    api(projects.frameworkPaperApi)
    api(projects.featureCommonApi)
}
