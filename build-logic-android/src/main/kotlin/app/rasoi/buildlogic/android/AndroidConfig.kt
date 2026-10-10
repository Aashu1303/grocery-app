package app.rasoi.buildlogic.android

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal object AndroidConfig {
    const val COMPILE_SDK = 36
    const val MIN_SDK = 26
    const val TARGET_SDK = 36
}

internal fun Project.androidNamespace(): String =
    "app.rasoi." + path.removePrefix(":").split(':').joinToString(".") { it.lowercase() }

internal fun Project.addAndroidTarget() {
    extensions.getByType(KotlinMultiplatformExtension::class.java).androidTarget {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }
}
