plugins {
    id("plutoproject.paper-conventions")
}

dependencies {
    api(project(":feature:whitelist-v2:api"))
    api(project(":feature:whitelist-v2:core"))
}
