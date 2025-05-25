plugins {
  kotlin("jvm")
  id("com.autonomousapps.dependency-analysis")
}

// Define Kotlin version and repositories in the root project's build.gradle.kts
// to ensure consistency across modules.
//
// tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
//   kotlinOptions {
//     jvmTarget = "1.8" // Or your desired JVM target
//   }
// }
//
// repositories {
//   mavenCentral()
// }
