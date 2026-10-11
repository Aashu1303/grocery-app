package app.rasoi.buildlogic.android

import org.gradle.api.Project

internal object AndroidConfig {
    const val COMPILE_SDK = 37
    const val MIN_SDK = 26
    const val TARGET_SDK = 37
}

internal fun Project.androidNamespace(): String =
    "app.rasoi." + path.removePrefix(":").split(':').joinToString(".") { it.lowercase() }
