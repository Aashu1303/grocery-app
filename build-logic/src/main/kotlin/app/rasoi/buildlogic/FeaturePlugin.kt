package app.rasoi.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class FeaturePlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(ComposeLibraryPlugin::class.java)
        configureExtension<KotlinMultiplatformExtension> {
            sourceSets.getByName("commonMain").dependencies {
                implementation(project(":core:model"))
                implementation(project(":core:domain"))
                implementation(project(":core:designsystem"))
                implementation(lib("kotlinx-coroutines-core"))
                implementation(lib("koin-core"))
            }
            sourceSets.getByName("commonTest").dependencies {
                implementation(project(":core:testing"))
            }
        }
    }
}
