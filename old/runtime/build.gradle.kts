subprojects {
    dependencies {
        compileOnly(project(":framework:api"))
    }
}

tasks.shadowJar {
    mergeServiceFiles()
}