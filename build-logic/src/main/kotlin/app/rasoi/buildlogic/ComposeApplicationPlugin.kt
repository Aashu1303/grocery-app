package app.rasoi.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

/** The shared app module: navigation and DI for all platforms, exported to iOS as a framework (ADR 0013). */
class ComposeApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(ComposeLibraryPlugin::class.java)
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
