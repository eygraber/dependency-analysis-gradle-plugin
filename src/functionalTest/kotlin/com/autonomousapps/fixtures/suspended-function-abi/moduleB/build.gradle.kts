plugins {
  kotlin("jvm")
  id("com.autonomousapps.dependency-analysis")
}

dependencies {
  api(project(":moduleA"))
  // Ensure kotlinx-coroutines-core is available for the suspend function compilation if needed,
  // though typically it's a runtime dependency. For interface definition, it might not be strictly necessary here,
  // but adding it to be safe or if the module had implementations.
  // implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") // Will be added to moduleC
}

// Define Kotlin version and repositories in the root project's build.gradle.kts
//
// tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
//   kotlinOptions {
//     jvmTarget = "1.8"
//   }
// }
//
// repositories {
//   mavenCentral()
// }
