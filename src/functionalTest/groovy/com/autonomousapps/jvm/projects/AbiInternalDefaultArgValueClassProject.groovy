// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

import static com.autonomousapps.kit.gradle.Dependency.project
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.kotlinStdLib

/**
 * An {@code internal} top-level function with a default argument whose type is an inline value class
 * wrapping a <em>nullable</em> reference.
 *
 * <p>Kotlin emits the function itself with a mangled name ({@code sat-<hash>}), which the ABI
 * analysis correctly recognizes as non-public. But it also emits a synthetic {@code $default}
 * bridge, and that bridge's descriptor keeps the <em>wrapper</em> type ({@code PathParent}) while
 * the Kotlin metadata records the function's parameter as the <em>erased</em> type
 * ({@code Path}, since a value class over a nullable reference does not erase to its underlying
 * type). {@code MethodBinarySignature.alternateDefaultSignature} reconstructs the non-bridge
 * signature by stripping {@code $default} and the trailing {@code (I, Object)} parameters, so it
 * looks up {@code sat-<hash>(LPathParent;)Z} — which does not exist in the metadata. The lookup
 * returns null, {@code findMemberVisibility} falls back to {@code ?: true}, and the bridge is
 * treated as public API.
 *
 * <p>The consequence is that the file facade is retained in the ABI even though every top-level
 * member in the file is {@code internal} or {@code private}.
 */
final class AbiInternalDefaultArgValueClassProject extends AbstractProject {

  final GradleProject gradleProject

  AbiInternalDefaultArgValueClassProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('lib') { s ->
        s.sources = libSources
        s.withBuildScript { bs ->
          bs.plugins = kotlin
          bs.dependencies = [kotlinStdLib('api')]
        }
      }
      .withSubproject('consumer') { s ->
        s.sources = consumerSources
        s.withBuildScript { bs ->
          bs.plugins = kotlin
          bs.dependencies = [
            kotlinStdLib('api'),
            project('implementation', ':lib'),
          ]
        }
      }
      .write()
  }

  private libSources = [
    new Source(
      SourceType.KOTLIN, 'Path', 'com/example',
      """\
        package com.example

        @JvmInline
        value class Path(val id: String)

        @JvmInline
        value class PathParent(private val path: Path?) {
          companion object {
            val Empty = PathParent(null)
          }
        }""".stripIndent()
    ),
  ]

  private consumerSources = [
    new Source(
      SourceType.KOTLIN, 'Use', 'com/example',
      """\
        package com.example

        internal fun sat(p: PathParent = PathParent.Empty): Boolean = p != PathParent.Empty

        internal class User {
          fun go() = sat()
        }""".stripIndent()
    ),
  ]

  /** Every top-level member of Use.kt is `internal`, so the facade must not appear in the ABI. */
  final List<String> expectedAbiDumpLines = []
}
