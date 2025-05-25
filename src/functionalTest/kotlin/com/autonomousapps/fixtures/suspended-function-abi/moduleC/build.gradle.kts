plugins {
  kotlin("jvm")
  id("com.autonomousapps.dependency-analysis")
}

dependencies {
  implementation(project(":moduleB"))
  implementation(project(":moduleA")) // This is initially included
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") // For runBlocking
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
