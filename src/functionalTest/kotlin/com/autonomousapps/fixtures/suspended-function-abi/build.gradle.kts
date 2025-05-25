plugins {
  kotlin("jvm") apply false
  id("com.autonomousapps.dependency-analysis") apply false
}

allprojects {
  repositories {
    mavenCentral() // Or your specific repository for the dependency-analysis plugin and other dependencies
    google() // For Android dependencies if any, though not strictly needed for this JVM case
  }
}

subprojects {
  apply(plugin = "org.jetbrains.kotlin.jvm")
  apply(plugin = "com.autonomousapps.dependency-analysis")

  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
      jvmTarget = "1.8" // Or your project's standard JVM target
      // Ensure language version is new enough for suspend functions, e.g., 1.3+
      languageVersion = "1.7" // Or your project's standard Kotlin language version
    }
  }

  // It's good practice to define versions in a central place,
  // but for this test fixture, direct specification is acceptable.
  // Ensure this aligns with the versions used in the main project or as needed for the test.
  dependencies {
    // kotlin("stdlib-jdk8") // This is usually added by the kotlin("jvm") plugin
  }
}

// Specific version for the dependency analysis plugin, if required for the test setup.
// This would typically be inherited from the main build's pluginManagement if running as part of a larger build.
// plugins {
//   id("com.autonomousapps.dependency-analysis") version "x.y.z" // Specify version if not inherited
// }

// Configure the dependency-analysis plugin if needed, for example:
// dependencyAnalysis {
//   // configuration options
// }
