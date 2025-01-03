subprojects {
    dependencies {
        compileOnly(project(":framework:api"))
        compileOnly(project(":menu:api"))
        compileOnly(parent!!.libs.spark.api)
    }
}