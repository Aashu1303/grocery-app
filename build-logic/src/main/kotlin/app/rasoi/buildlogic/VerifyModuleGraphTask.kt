package app.rasoi.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class VerifyModuleGraphTask : DefaultTask() {
    @get:Input
    abstract val edges: MapProperty<String, Set<String>>

    @TaskAction
    fun verify() {
        val violations = ModuleRules.violations(edges.get())
        if (violations.isNotEmpty()) throw GradleException("Module rule violations:\n" + violations.joinToString("\n"))
    }
}
