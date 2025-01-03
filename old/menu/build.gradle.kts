subprojects {
    dependencies {
        compileOnly(project(":framework:api"))
        compileOnly(rootProject.libs.coreprotect)
    }
}