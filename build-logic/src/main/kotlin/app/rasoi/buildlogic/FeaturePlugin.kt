package app.rasoi.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * A feature module: Compose UI on design-system components, MVI ViewModels, its own string resources
 * (`app.rasoi.feature.<name>.resources`) and desktop screenshot tests gated like the design system.
 */
class FeaturePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(ComposeLibraryPlugin::class.java)
            pluginManager.apply("io.github.takahirom.roborazzi")
            applyKoinCompiler()
            val compose = extensions.getByType(ComposeExtension::class.java)
            val desktopRuntime = compose.dependencies.desktop.currentOs
            configureExtension<KotlinMultiplatformExtension> {
                sourceSets.getByName("commonMain").dependencies {
                    implementation(project(":core:model"))
                    implementation(project(":core:domain"))
                    implementation(project(":core:designsystem"))
                    implementation(lib("kotlinx-coroutines-core"))
                    implementation(lib("koin-core"))
                    implementation(lib("koin-annotations"))
                    implementation(lib("koin-compose-viewmodel"))
                    implementation(lib("lifecycle-viewmodel"))
                    implementation(lib("lifecycle-viewmodel-compose"))
                    implementation(lib("lifecycle-runtime-compose"))
                    implementation(lib("compose-runtime"))
                    implementation(lib("compose-foundation"))
                    implementation(lib("compose-ui"))
                    implementation(lib("compose-resources"))
                }
                sourceSets.getByName("commonTest").dependencies {
                    implementation(project(":core:testing"))
                }
                sourceSets.getByName("jvmTest").dependencies {
                    implementation(lib("compose-ui-test"))
                    implementation(lib("compose-ui-test-junit4"))
                    implementation(lib("roborazzi-compose-desktop"))
                    implementation(lib("kotlinx-coroutines-swing"))
                    implementation(lib("junit4"))
                    implementation(desktopRuntime)
                }
            }
            compose.extensions.getByType(ResourcesExtension::class.java)
                .apply {
                    publicResClass = false
                    packageOfResClass = "app.rasoi.feature.${name.lowercase()}.resources"
                    generateResClass = ResourcesExtension.ResourceClassGeneration.Always
                }
            tasks.named("check").configure { it.dependsOn("verifyRoborazziJvm") }
        }
    }
}
