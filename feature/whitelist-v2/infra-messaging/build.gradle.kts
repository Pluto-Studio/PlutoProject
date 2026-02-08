plugins {
    id("plutoproject.core-conventions")
}

dependencies {
    api(project(":feature:whitelist-v2:application"))

    api(project(":framework-common-api"))
}
