import xyz.jpenilla.resourcefactory.paper.PaperPluginYaml.Load

plugins {
    id("plutoproject.platform-conventions")
    id("plutoproject.paper-conventions")
    alias(libs.plugins.resourceFactory.paper)
}

dependencies {
    api(projects.frameworkPaper)
    api(projects.featurePaper)
    implementation(project(":kernel:paper"))
    implementation(project(":capability:mongo:paper"))
    implementation(project(":capability:server-identifier:paper"))
    implementation(project(":capability:database-persist:paper"))
    implementation(project(":capability:profile:paper"))
    implementation(project(":capability:interactive:paper"))
    implementation(project(":capability:server-statistics:paper"))
    implementation(project(":capability:world-alias:paper"))
}

paperPluginYaml {
    name = "PlutoProject"
    main = "plutoproject.platform.paper.PlutoPaperPlatform"
    apiVersion = "26.2"
    author = "Pluto Studio"
    description = "A collection of framework and feature components for the PlutoProject server."
    dependencies {
        server(
            name = "spark",
            load = Load.BEFORE,
            required = false, joinClasspath = true
        )
        server(
            name = "Vault",
            load = Load.BEFORE,
            required = false, joinClasspath = true
        )
        server(
            name = "CoreProtect",
            load = Load.BEFORE,
            required = false, joinClasspath = true
        )
    }
}
