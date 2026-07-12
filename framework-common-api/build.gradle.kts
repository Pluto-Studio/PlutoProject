plugins {
    id("plutoproject.common-conventions")
    id("plutoproject.dokka-conventions")
}

dependencies {
    api(project(":foundation:common"))
}
