pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral() // Or your specific repository for the dependency-analysis plugin
  }
}

rootProject.name = "suspended-function-abi-test"

include(":moduleA")
include(":moduleB")
include(":moduleC")
