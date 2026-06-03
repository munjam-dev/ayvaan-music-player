// Top-level build file where you can add configuration options common to all sub-projects/modules.
tasks.register("listFiles") {
    notCompatibleWithConfigurationCache("Custom task")
    doLast {
        val root = java.io.File("/")
        root.walkTopDown().maxDepth(4).filter { it.extension == "png" || it.extension == "jpg" }.forEach {
            println("FOUND FILE: ${it.absolutePath}")
        }
    }
}
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
}
