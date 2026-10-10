package app.rasoi.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
        pluginManager.apply(QualityPlugin::class.java)
        group = "app.rasoi"

        configureExtension<KotlinMultiplatformExtension> {
            jvm { compilerOptions.jvmTarget.set(JvmTarget.JVM_17) }
            if (iosEnabled) {
                iosArm64()
                iosSimulatorArm64()
            }
            compilerOptions {
                allWarningsAsErrors.set(true)
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
            sourceSets.getByName("commonTest").dependencies {
                implementation(kotlin("test"))
                implementation(lib("kotlinx-coroutines-test"))
                implementation(lib("turbine"))
            }
        }
        if (androidEnabled) pluginManager.apply("rasoi.android-library")
    }
}
