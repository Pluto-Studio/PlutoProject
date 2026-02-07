plugins {
    id("plutoproject.velocity-conventions")
}

dependencies {
    api(project(":feature:whitelist-v2:api"))
    api(project(":feature:whitelist-v2:core"))

    api(project(":feature:whitelist-v2:infra-mongo"))
    api(project(":feature:whitelist-v2:infra-messaging"))

    api(project(":framework-common-api"))
    api(project(":framework-velocity-api"))
    api(project(":feature-velocity-api"))
}
