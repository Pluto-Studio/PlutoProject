subprojects {
    dependencies {
        compileOnly(project(":framework:api"))
        compileOnly(project(":menu:api"))
    }
}