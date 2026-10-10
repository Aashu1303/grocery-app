import org.gradle.api.Project

val Project.androidEnabled: Boolean get() = providers.gradleProperty("rasoi.android").orNull.toBoolean()
val Project.iosEnabled: Boolean get() = providers.gradleProperty("rasoi.ios").orNull.toBoolean()

fun Project.catalog() = extensions
    .getByType(org.gradle.api.artifacts.VersionCatalogsExtension::class.java)
    .named("libs")
