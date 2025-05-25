package com.autonomousapps

import com.autonomousapps.fixtures.ProjectDirProvider
import com.autonomousapps.internal.advice.AdvicePrinter
import com.autonomousapps.internal.getAdviceService
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SuspendedFunctionAbiTest {

  @TempDir
  lateinit var projectDir: File

  private lateinit var project: ProjectDirProvider

  private val gradleRunner = GradleRunner.create()
    .withDebug(true) // Run with --debug
    .withPluginClasspath()

  @BeforeEach fun setup() {
    project = ProjectDirProvider(projectDir.toPath().resolve("suspended-function-abi"))
    project.writeVersion(System.getProperty("com.autonomousapps.pluginversion"))
    // Assuming the fixture files are already created in the location specified by 'project.projectDir'
    // by previous steps or a setup script.
    // If not, they need to be copied/generated here. For this plan, we assume they are in:
    // src/functionalTest/kotlin/com/autonomousapps/fixtures/suspended-function-abi/
    // And ProjectDirProvider will copy them to the tempDir.

    // The ProjectDirProvider is expected to copy the fixture from 
    // "src/functionalTest/kotlin/com/autonomousapps/fixtures/suspended-function-abi"
    // to the temporary test directory.
  }

  @Test fun `suspended function ABI changes are handled correctly`() {
    val build = gradleRunner.withProjectDir(project.projectDir.toFile())
      .withArguments("buildHealth")
      .build()

    // Check if the task was successful
    // assert(build.task(":moduleC:buildHealth")?.outcome == TaskOutcome.SUCCESS) // Or the aggregate task

    val adviceService = getAdviceService(project.projectDir.toFile())
    val projectAdvice = adviceService.getProjectAdvice()

    // Assertion 1: Verify that the advice for moduleB does NOT recommend changing moduleA from api to implementation.
    val moduleBAdvice = projectAdvice.find { it.projectName == ":moduleB" }
    assert(moduleBAdvice != null) { "Advice for :moduleB should exist" }

    val moduleADependencyAdviceInB = moduleBAdvice!!.dependencyAdvice.find { it.dependency.identifier == ":moduleA" }
    assert(moduleADependencyAdviceInB != null) { "Advice for :moduleA in :moduleB should exist" }
    assert(moduleADependencyAdviceInB!!.currentConfiguration == "api") {
      "moduleB's dependency on moduleA should be 'api'. Was ${moduleADependencyAdviceInB.currentConfiguration}"
    }
    // This is the core of the assertion: there should be no advice to change, or if there is, it's not to 'implementation'
    assert(moduleADependencyAdviceInB.advisedConfiguration == null || moduleADependencyAdviceInB.advisedConfiguration == "api") {
      "moduleB's dependency on moduleA should remain 'api'. Got advice to change to ${moduleADependencyAdviceInB.advisedConfiguration}. Report: \n${AdvicePrinter(moduleBAdvice).asText()}"
    }


    // Assertion 2: Verify that the advice for moduleC does NOT recommend removing its implementation dependency on moduleA.
    val moduleCAdvice = projectAdvice.find { it.projectName == ":moduleC" }
    assert(moduleCAdvice != null) { "Advice for :moduleC should exist" }

    val moduleADependencyAdviceInC = moduleCAdvice!!.dependencyAdvice.find { it.dependency.identifier == ":moduleA" }
    assert(moduleADependencyAdviceInC != null) { "Advice for :moduleA in :moduleC should exist. Report: \n${AdvicePrinter(moduleCAdvice).asText()}" }
    assert(moduleADependencyAdviceInC!!.currentConfiguration == "implementation") {
      "moduleC's dependency on moduleA should be 'implementation'. Was ${moduleADependencyAdviceInC.currentConfiguration}"
    }
    // This is the core of the assertion: there should be no advice to remove this dependency
    assert(moduleADependencyAdviceInC.advisedConfiguration == null || moduleADependencyAdviceInC.advisedConfiguration == "implementation") {
      "moduleC's dependency on moduleA should not be advised for removal. Got advice to change to ${moduleADependencyAdviceInC.advisedConfiguration}. Report: \n${AdvicePrinter(moduleCAdvice).asText()}"
    }
  }
}
