// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.AbiInternalDefaultArgValueClassProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class AbiInternalDefaultArgValueClassSpec extends AbstractJvmSpec {

  def "synthetic \$default bridge of an internal function is not public ABI (#gradleVersion)"() {
    given:
    def project = new AbiInternalDefaultArgValueClassProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':consumer:abiAnalysisMain')

    then: 'the file facade is absent from the ABI, since every top-level member is internal'
    def abiDump = gradleProject.singleArtifact('consumer', 'reports/dependency-analysis/main/abi-dump.txt')
    assertThat(abiDump.exists()).isTrue()
    def lines = abiDump.asFile.text.trim().with { it.isEmpty() ? [] : it.readLines() }
    assertThat(lines).containsExactlyElementsIn(project.expectedAbiDumpLines)

    where:
    gradleVersion << gradleVersions()
  }
}
