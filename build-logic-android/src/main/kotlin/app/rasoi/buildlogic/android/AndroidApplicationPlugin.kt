package app.rasoi.buildlogic.android

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project

/** The thin Android application module that hosts the shared Compose UI (ADR 0013). */
class AndroidApplicationPlugin : Plugin<Project> {
    private companion object {
        /** The host machine as seen from the Android emulator. */
        const val DEFAULT_API_BASE_URL = "http://10.0.2.2:8080"
    }

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            extensions.getByType(ApplicationExtension::class.java).apply {
                namespace = "app.rasoi.android"
                compileSdk = AndroidConfig.COMPILE_SDK
                defaultConfig.apply {
                    applicationId = "app.rasoi"
                    minSdk = AndroidConfig.MIN_SDK
                    targetSdk = AndroidConfig.TARGET_SDK
                    versionCode = 1
                    versionName = "0.1.0"
                    val apiBaseUrl = providers.gradleProperty("rasoi.apiBaseUrl").orNull ?: DEFAULT_API_BASE_URL
                    buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
                }
                compileOptions.sourceCompatibility = JavaVersion.VERSION_17
                compileOptions.targetCompatibility = JavaVersion.VERSION_17
                buildFeatures.compose = true
                buildFeatures.buildConfig = true
                buildTypes.getByName("release").isMinifyEnabled = false
            }
        }
    }
}
