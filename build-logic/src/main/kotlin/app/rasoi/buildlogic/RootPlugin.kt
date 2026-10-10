package app.rasoi.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

class RootPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("base")
        val moduleGraph = tasks.register("verifyModuleGraph", VerifyModuleGraphTask::class.java)
        val featureSources = tasks.register("verifyFeatureSources", VerifyFeatureSourcesTask::class.java) { task ->
            task.rootDirectory.set(layout.projectDirectory)
            task.sources.from(
                fileTree("feature") {
                    it.include("*/src/*/kotlin/**/*.kt")
                    it.exclude("*/src/*Test/**")
                },
            )
        }
        gradle.projectsEvaluated {
            val edges = subprojects.associate { project -> project.path to productionDependencies(project) }
            moduleGraph.configure { it.edges.set(edges) }
        }
        tasks.named("check").configure { it.dependsOn(moduleGraph, featureSources) }
    }

    private fun productionDependencies(project: Project): Set<String> = project.configurations
        .filterNot { it.name.contains("test", ignoreCase = true) }
        .flatMap { configuration -> configuration.dependencies.withType(ProjectDependency::class.java).map { it.path } }
        .toSet() - project.path
}
