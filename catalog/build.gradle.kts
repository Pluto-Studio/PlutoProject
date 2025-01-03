plugins {
    id("version-catalog")
}

catalog {
    versionCatalog {
        from(rootProject.files("gradle/libs.versions.toml"))
    }
}