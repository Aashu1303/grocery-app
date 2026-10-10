package app.rasoi.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class ComposeLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(KmpLibraryPlugin::class.java)
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        pluginManager.apply("org.jetbrains.compose")
    }
}
