pluginManagement {
    val android = providers.gradleProperty("rasoi.android").orNull.toBoolean()
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
        if (android) google()
    }
}

val androidEnabled = providers.gradleProperty("rasoi.android").orNull.toBoolean()
val composeEnabled = providers.gradleProperty("rasoi.compose").orNull.toBoolean()

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
        if (androidEnabled || composeEnabled) google()
    }
}

rootProject.name = "rasoi"

// Android conventions reach every project through the root buildscript classpath (see build.gradle.kts).
if (androidEnabled) includeBuild("build-logic-android")

include(
    ":core:model",
    ":core:domain",
    ":core:data",
    ":core:contract",
    ":core:testing",
    ":server:app",
    ":server:parsing",
    ":server:jobs",
    ":server:notifications",
)

// Compose modules need androidx artifacts that are published only to Google Maven.
if (composeEnabled) {
    include(
        ":composeApp",
        ":core:designsystem",
        ":feature:household",
        ":feature:pantry",
        ":feature:invoiceimport",
        ":feature:bills",
        ":feature:ledger",
        ":feature:kitchen",
    )
}
