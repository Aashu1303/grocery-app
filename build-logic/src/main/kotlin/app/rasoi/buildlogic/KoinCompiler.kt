package app.rasoi.buildlogic

import org.gradle.api.Project
import org.koin.compiler.plugin.KoinGradleExtension

/**
 * Koin's compiler plugin checks the dependency graph at build time (replaces runtime-only failures). It has not yet
 * verified Kotlin 2.4.21 and reports that as a warning, which this build treats as an error; the check is lowered to
 * info until the plugin lists the Kotlin version we use.
 */
internal fun Project.applyKoinCompiler() {
    pluginManager.apply("io.insert-koin.compiler.plugin")
    configureExtension<KoinGradleExtension> {
        versionCheckSeverity.set("INFO")
        logSeverity.set("INFO")
    }
}
