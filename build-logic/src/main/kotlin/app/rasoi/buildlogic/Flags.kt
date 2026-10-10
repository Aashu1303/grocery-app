package app.rasoi.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider

internal val Project.androidEnabled: Boolean
    get() = providers.gradleProperty("rasoi.android").orNull.toBoolean()

internal val Project.iosEnabled: Boolean
    get() = providers.gradleProperty("rasoi.ios").orNull?.toBoolean()
        ?: System.getProperty("os.name").startsWith("Mac")

internal val Project.libs: VersionCatalog
    get() = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

internal fun Project.lib(alias: String): Provider<*> = libs.findLibrary(alias).get()

internal inline fun <reified T : Any> Project.configureExtension(block: T.() -> Unit) {
    extensions.getByType(T::class.java).block()
}
