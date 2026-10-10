package app.rasoi.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

/** The app entry module: Compose Multiplatform with an Android application and an iOS framework. */
class ComposeApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configureKotlinMultiplatform()
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            pluginManager.apply("org.jetbrains.compose")
            if (androidEnabled) pluginManager.apply("rasoi.android-application")
            configureExtension<KotlinMultiplatformExtension> {
                targets.withType(KotlinNativeTarget::class.java).configureEach { native ->
                    native.binaries.framework {
                        baseName = "ComposeApp"
                        isStatic = true
                    }
                }
            }
        }
    }
}
