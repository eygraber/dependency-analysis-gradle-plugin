// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp

import com.autonomousapps.kmp.projects.ComposeMultiplatformProject
import spock.lang.Issue

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class ComposeMultiplatformSpec extends AbstractKmpSpec {

  @Issue("https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1783#issuecomment-5093165024")
  def "does not advise removing a commonMain dependency that resolves to a different group on android (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new ComposeMultiplatformProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
