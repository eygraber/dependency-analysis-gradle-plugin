// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp

import com.autonomousapps.kmp.projects.ImplicitKotlinTestJunitProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class ImplicitKotlinTestJunitSpec extends AbstractKmpSpec {

  def "advises kotlin-test-junit on androidHostTest instead of commonTest with iOS targets (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new ImplicitKotlinTestJunitProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
