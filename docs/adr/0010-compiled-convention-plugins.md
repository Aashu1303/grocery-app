# 0010. Convention plugins are compiled Kotlin classes

Status: Accepted
Date: 2026-10-10

## Context
Gradle 8.14 compiles `kotlin-dsl` precompiled script plugins with its embedded Kotlin 2.0 compiler, which cannot
read the Kotlin 2.4 metadata of the Kotlin, Compose, SQLDelight and Roborazzi Gradle plugins.

## Decision
`build-logic` and `build-logic-android` use `java-gradle-plugin` with the project's Kotlin version. Each convention
is a `Plugin<Project>` class registered by id (`rasoi.kmp-library`, `rasoi.feature`, ...). Module build files stay
in the Kotlin DSL. Rule logic (module graph, design-system source rules) is plain Kotlin with unit tests.

## Consequences
Convention code is typed and testable. It is slightly more verbose than script plugins (explicit extension lookups).
