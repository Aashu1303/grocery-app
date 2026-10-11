package app.rasoi.buildlogic.android

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project

/** The thin Android application module that hosts the shared Compose UI (ADR 0013). */
class AndroidApplicationPlugin : Plugin<Project> {
    private companion object {
        /** The host machine as seen from the Android emulator. */
        const val DEFAULT_API_BASE_URL = "http://10.0.2.2:8080"
        const val HTTPS = "https://"

        val FIREBASE_PROPERTIES = mapOf(
            "FIREBASE_APP_ID" to "rasoi.firebase.appId",
            "FIREBASE_API_KEY" to "rasoi.firebase.apiKey",
            "FIREBASE_PROJECT_ID" to "rasoi.firebase.projectId",
        )
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
                configureRelease(this)
            }
            requireHttpsForRelease()
        }
    }

    /**
     * Release builds sign with a key supplied through the environment, and carry the Firebase project's identifiers
     * from Gradle properties (ADR 0024); nothing secret is committed. Shrinking stays off until a device-tested set of
     * keep rules exists.
     */
    private fun Project.configureRelease(android: ApplicationExtension) {
        val keystore = providers.environmentVariable("RASOI_KEYSTORE_FILE").orNull
        if (keystore != null) {
            val signing = android.signingConfigs.create("release")
            signing.storeFile = file(keystore)
            signing.storePassword = providers.environmentVariable("RASOI_KEYSTORE_PASSWORD").orNull
            signing.keyAlias = providers.environmentVariable("RASOI_KEY_ALIAS").orNull
            signing.keyPassword = providers.environmentVariable("RASOI_KEY_PASSWORD").orNull
            android.buildTypes.getByName("release").signingConfig = signing
        }
        val release = android.buildTypes.getByName("release")
        release.isMinifyEnabled = false
        FIREBASE_PROPERTIES.forEach { (field, property) ->
            val value = providers.gradleProperty(property).orNull.orEmpty()
            release.buildConfigField("String", field, "\"$value\"")
        }
    }

    /** Plain HTTP is a development convenience; a release must point at an HTTPS server. */
    private fun Project.requireHttpsForRelease() {
        val apiBaseUrl = providers.gradleProperty("rasoi.apiBaseUrl").orNull
        tasks.matching { it.name == "preReleaseBuild" }.configureEach { task ->
            task.doFirst {
                if (apiBaseUrl?.startsWith(HTTPS) != true) {
                    throw GradleException("Release builds need -Prasoi.apiBaseUrl=https://<your server>")
                }
            }
        }
    }
}
