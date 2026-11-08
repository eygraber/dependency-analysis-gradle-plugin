// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.kotlin.KotlinNativeTarget
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.implementation

final class ImplicitKotlinTestJunitProject extends AbstractKmpProject {

  static final String KOTLIN_TEST_JUNIT = "org.jetbrains.kotlin:kotlin-test-junit:${getKotlinVersion()}"

  final GradleProject gradleProject

  ImplicitKotlinTestJunitProject(agpVersion) {
    super(getKotlinVersion(), agpVersion as String)
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withAndroidKmpLibProject('consumer') { s ->
        s.sources = consumerSources()
        s.withBuildScript { bs ->
          bs.plugins = androidKmpLibrary
          bs.kotlinKmp { k ->
            k.nativeTargets = [KotlinNativeTarget.iosArm64(), KotlinNativeTarget.iosSimulatorArm64()]
            k.androidLibrary { a ->
              a.namespace = 'dagp.consumer'
              a.compileSdk = 33
              a.minSdk = 24
              a.withHostTest()
            }
            k.sourceSets { sourceSets ->
              sourceSets.commonTest { commonTest ->
                commonTest.dependencies(
                  implementation('kotlin("test")'),
                )
              }
            }
          }
        }
      }
      .write()
  }

  private static List<Source> consumerSources() {
    return [
      Source
        .kotlin(
          '''
            package consumer.common.main

            class CommonMain
          '''
        )
        .withSourceSet('commonMain')
        .build(),
      Source
        .kotlin(
          '''
            package consumer.common.test

            import consumer.common.main.CommonMain
            import kotlin.test.Test
            import kotlin.test.assertNotNull

            class CommonTest {
              @Test fun test() {
                assertNotNull(CommonMain())
              }
            }
          '''
        )
        .withSourceSet('commonTest')
        .build(),
    ]
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> consumerAdvice = [
    Advice.ofAdd(moduleCoordinates(KOTLIN_TEST_JUNIT), 'androidHostTestImplementation'),
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':consumer', consumerAdvice),
  ]
}
