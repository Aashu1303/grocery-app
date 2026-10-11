package app.rasoi.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class VerifyFeatureSourcesTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    @TaskAction
    fun verify() {
        val root = rootDirectory.get().asFile
        val violations = sources.files.sorted().flatMap { file ->
            SourceRules.violations(file.relativeTo(root).path, file.readText())
        }
        if (violations.isNotEmpty()) {
            throw GradleException("Design-system violations:\n" + violations.joinToString("\n"))
        }
    }
}
