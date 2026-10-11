package app.rasoi.buildlogic.android

import org.gradle.api.Project

internal object AndroidConfig {
    const val COMPILE_SDK = 36
    const val MIN_SDK = 26
    const val TARGET_SDK = 36
}

internal fun Project.androidNamespace(): String =
    "app.rasoi." + path.removePrefix(":").split(':').joinToString(".") { it.lowercase() }
