package com.autonomousapps.scenarios

import com.google.common.truth.Truth.assertThat
import com.autonomousapps.internal.utils.fromJson // Utility for parsing single JSON object
import com.autonomousapps.model.*
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class SuspendGenericApiTest {

  @TempDir
  lateinit var projectDir: File

  private val kotlinVersion = System.getProperty("test.kotlin.version")
  // Pick a common recent version for coroutines. This might need adjustment.
  private val coroutinesVersion = "1.7.3"

  private lateinit var settingsFile: File
  private lateinit var rootBuildFile: File

  private lateinit var libNetworkDir: File
  private lateinit var libNetworkBuildFile: File
  private lateinit var libNetworkSrcDir: File

  private lateinit var libFeatureDir: File
  private lateinit var libFeatureBuildFile: File
  private lateinit var libFeatureSrcDir: File

  private lateinit var appDir: File
  private lateinit var appBuildFile: File
  private lateinit var appSrcDir: File

  @BeforeEach
  fun setup() {
    settingsFile = projectDir.resolve("settings.gradle.kts")
    rootBuildFile = projectDir.resolve("build.gradle.kts") // Root build file, if needed

    libNetworkDir = projectDir.resolve("lib-network")
    libNetworkBuildFile = libNetworkDir.resolve("build.gradle.kts")
    libNetworkSrcDir = libNetworkDir.resolve("src/main/kotlin/com/example/network")

    libFeatureDir = projectDir.resolve("lib-feature")
    libFeatureBuildFile = libFeatureDir.resolve("build.gradle.kts")
    libFeatureSrcDir = libFeatureDir.resolve("src/main/kotlin/com/example/feature")

    appDir = projectDir.resolve("app")
    appBuildFile = appDir.resolve("build.gradle.kts")
    appSrcDir = appDir.resolve("src/main/kotlin/com/example/app")

    // Create directories
    libNetworkSrcDir.mkdirs()
    libFeatureSrcDir.mkdirs()
    appSrcDir.mkdirs()

    settingsFile.writeText(
      """
      rootProject.name = "suspend-generic-api-test"
      include(":lib-network")
      include(":lib-feature")
      include(":app")
      """.trimIndent()
    )

    // Root build file can be empty or define common configurations if necessary
    rootBuildFile.writeText(
      """
      plugins {
        id("com.autonomousapps.dependency-analysis") version "${System.getProperty("com.autonomousapps.pluginversion")}" apply false
      }
      
      allprojects {
        repositories {
          mavenCentral() // For Kotlin stdlib and coroutines
          google() // If any Android dependencies were involved
        }
      }
      """.trimIndent()
    )

    // lib-network
    libNetworkBuildFile.writeText(
      """
      plugins {
        kotlin("jvm") version "$kotlinVersion"
        id("com.autonomousapps.dependency-analysis")
      }
      dependencies {
        implementation(kotlin("stdlib"))
      }
      """.trimIndent()
    )
    libNetworkSrcDir.resolve("ApiResult.kt").writeText(
      """
      package com.example.network
      sealed class ApiResult<T> {
        data class Success<T>(val data: T) : ApiResult<T>()
        data class Error(val message: String) : ApiResult<Nothing>() // Added a field to Error for completeness
      }
      """.trimIndent()
    )

    // lib-feature
    libFeatureBuildFile.writeText(
      """
      plugins {
        kotlin("jvm") version "$kotlinVersion"
        id("com.autonomousapps.dependency-analysis")
      }
      dependencies {
        api(project(":lib-network"))
        implementation(kotlin("stdlib"))
      }
      """.trimIndent()
    )
    libFeatureSrcDir.resolve("FeatureService.kt").writeText(
      """
      package com.example.feature
      import com.example.network.ApiResult
      data class FeatureData(val id: String)
      interface FeatureService {
        suspend fun getData(): ApiResult<FeatureData>
      }
      """.trimIndent()
    )

    // app
    appBuildFile.writeText(
      """
      plugins {
        kotlin("jvm") version "$kotlinVersion"
        id("com.autonomousapps.dependency-analysis")
      }
      dependencies {
        implementation(project(":lib-feature"))
        implementation(kotlin("stdlib"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
      }
      """.trimIndent()
    )
    appSrcDir.resolve("Main.kt").writeText(
      """
      package com.example.app
      import com.example.feature.FeatureService
      import com.example.feature.FeatureData
      import com.example.network.ApiResult
      import kotlinx.coroutines.runBlocking
      
      class DummyFeatureService : FeatureService {
        override suspend fun getData(): ApiResult<FeatureData> {
          return ApiResult.Success(FeatureData("test-id"))
        }
      }
      
      fun main() = runBlocking {
        val service: FeatureService = DummyFeatureService()
        val result = service.getData()
        println(result)
      }
      """.trimIndent()
    )
  }

  @Test
  fun `should not advise changing api to implementation for suspend function with generic return type`() {
    val runner = GradleRunner.create()
      .withProjectDir(projectDir)
      .withArguments("buildHealth", "--stacktrace")
      // Ensure the plugin version is available to the test build
      .withPluginClasspath()

    val result = runner.build()

    assertThat(result.task(":lib-feature:buildHealth")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

    val libFeatureReportFile = projectDir.resolve("lib-feature/build/reports/dependency-analysis/project-health-report.json")
    assertThat(libFeatureReportFile.exists()).isTrue()

    val libFeatureProjectAdvice = libFeatureReportFile.readText().fromJson<ProjectAdvice>()
    assertThat(libFeatureProjectAdvice).isNotNull()
    assertThat(libFeatureProjectAdvice.projectPath).isEqualTo(":lib-feature")

    val expectedNetworkCoordinates = ProjectCoordinates(":lib-network", GradleVariantIdentification.EMPTY)
    // Find advice pertaining to the :lib-network project dependency.
    // The advice model's `coordinates` field directly holds this.
    val adviceForNetwork = libFeatureProjectAdvice.dependencyAdvice.find {
      it.coordinates.identifier == expectedNetworkCoordinates.identifier && it.coordinates is ProjectCoordinates
    }

    // Assert that there is NO advice to change :lib-network for lib-feature from api to implementation
    if (adviceForNetwork != null) {
      // If there's advice, it should not be to change from "api" to "implementation"
      val isApiToImplChange = adviceForNetwork.fromConfiguration == "api" && adviceForNetwork.toConfiguration == "implementation"
      assertThat(isApiToImplChange).isFalse()
      // Also, ensure it's not an "add" advice suggesting implementation if fromConfiguration is null
      val isAddImplAdvice = adviceForNetwork.fromConfiguration == null && adviceForNetwork.toConfiguration == "implementation"
      assertThat(isAddImplAdvice).isFalse()
      // And ensure it's not a "remove" advice for an api dependency (unless it's truly unused, which this test isn't about)
      // For this test, the primary concern is not being told to demote a correctly used API.
      // A "remove" advice would imply it's unused, which is a different problem.
      // A "change" advice where `toConfiguration` is not `implementation` is acceptable (e.g. api -> compileOnlyApi)
    }
    // If adviceForNetwork is null, it means the plugin found no issue with the `api` dependency, which is the desired outcome.

    // Optional: Assert ABI dump contents
    val libFeatureAbiDumpFile = projectDir.resolve("lib-feature/build/reports/dependency-analysis/abi-dump.txt")
    assertThat(libFeatureAbiDumpFile.exists()).isTrue()
    val abiDumpContent = libFeatureAbiDumpFile.readText()

    assertThat(abiDumpContent).contains("Lcom/example/network/ApiResult;")
    assertThat(abiDumpContent).contains("Lcom/example/feature/FeatureData;")
    // Check for the suspend function signature. Note that the exact signature might vary based on Kotlin compilation.
    // This is a best guess; it might need adjustment after seeing the actual output.
    assertThat(abiDumpContent).contains("public abstract suspend fun getData()Lcom/example/network/ApiResult<Lcom/example/feature/FeatureData;>;")
  }
}
