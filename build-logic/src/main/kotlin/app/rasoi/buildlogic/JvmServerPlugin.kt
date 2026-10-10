package app.rasoi.buildlogic

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class JvmServerPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
        pluginManager.apply(QualityPlugin::class.java)
        group = "app.rasoi"

        configureExtension<KotlinJvmProjectExtension> {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
                allWarningsAsErrors.set(true)
            }
        }
        configureExtension<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        dependencies.add("testImplementation", "org.jetbrains.kotlin:kotlin-test")
        dependencies.add("testImplementation", lib("kotlinx-coroutines-test"))
        tasks.withType(Test::class.java).configureEach { it.useJUnitPlatform() }
    }
}
