// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.moduleCoordinates
import static com.autonomousapps.AdviceHelper.projectAdviceForDependencies
import static com.autonomousapps.kit.gradle.Dependency.implementation

/**
 * A KMP project with an Android target that declares a Compose Multiplatform dependency in {@code commonMain}. On the
 * Android classpath, that dependency redirects to its AndroidX counterpart, which lives in a <em>different group</em>:
 *
 * <pre>
 *   org.jetbrains.compose.runtime:runtime
 *   \--- androidx.compose.runtime:runtime
 *         \--- androidx.compose.runtime:runtime-android  (holds the bytecode)
 * </pre>
 *
 * All usage is therefore attributed to the AndroidX artifact, and DAGP emits contradictory advice: remove the
 * JetBrains-coordinate declaration from {@code commonMain}, and add the AndroidX artifact to {@code androidMain}.
 * Following it doesn't work — {@code commonMain} can't see {@code androidMain}'s dependencies, and the AndroidX
 * artifact is only reachable via the declaration DAGP wants removed.
 *
 * <p>{@code implicitKmpBundleFor}/{@code implicitKmpPrimaryFor} already handle this when the redirect stays within a
 * group ({@code foo} -> {@code foo-android}), but the suffix heuristic can't see across the group boundary.
 *
 * <p>Note this uses {@code compose.runtime} rather than {@code compose.foundation} from the issue report so that the
 * fixture doesn't need the Compose compiler plugin: {@code mutableStateOf} is not a {@code @Composable}.
 *
 * @see <a href="https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1783#issuecomment-5093165024">Issue 1783</a>
 */
final class ComposeMultiplatformProject extends AbstractProject {

  private static final String KOTLIN_VERSION = '2.2.21'

  static final String COMPOSE_RUNTIME = 'org.jetbrains.compose.runtime:runtime:1.9.3'

  final GradleProject gradleProject

  ComposeMultiplatformProject(agpVersion) {
    super(KOTLIN_VERSION, agpVersion as String)
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder(GradleProject.DslKind.KOTLIN)
      .withRootProject { r ->
        r.withBuildScript { bs ->
          bs.plugins(
            plugins.dependencyAnalysis,
            plugins.kotlinMultiplatformNoApply,
            plugins.androidKmpLibNoApply,
          )
        }
      }
      .withAndroidKmpLibProject('consumer') { s ->
        s.sources = consumerSources()
        s.withBuildScript { bs ->
          bs.plugins = androidKmpLibrary
          bs.kotlinKmp { k ->
            k.androidLibrary { a ->
              a.namespace = 'dagp.test'
              // androidx.compose.runtime:runtime-android declares minCompileSdk=34
              a.compileSdk = 34
              a.minSdk = 24
            }
            k.sourceSets { sourceSets ->
              sourceSets.commonMain { commonMain ->
                commonMain.dependencies(
                  implementation(COMPOSE_RUNTIME),
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
            package common.main

            import androidx.compose.runtime.mutableStateOf

            class CommonMain {
              private val state = mutableStateOf("magic")

              fun magicWord(): String = state.value
            }
          '''
        )
        .withSourceSet('commonMain')
        .build(),
    ]
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  /**
   * The only advice we want is the (unrelated) runtimeOnly declaration for the coroutines Android artifact, which
   * arrives transitively via {@code androidx.compose.runtime:runtime-android} and supplies {@code Dispatchers.Main} via
   * ServiceLoader.
   *
   * <p>Today we additionally get this contradictory pair, which is the bug:
   * <pre>
   *   Advice.ofRemove(moduleCoordinates(COMPOSE_RUNTIME), 'commonMainImplementation')
   *   Advice.ofAdd(moduleCoordinates('androidx.compose.runtime:runtime', '1.9.4'), 'androidMainImplementation')
   * </pre>
   */
  private final Set<Advice> consumerAdvice = [
    Advice.ofAdd(
      moduleCoordinates('org.jetbrains.kotlinx:kotlinx-coroutines-android', '1.8.1'),
      'androidMainRuntimeOnly'
    ),
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':consumer', consumerAdvice),
  ]
}
