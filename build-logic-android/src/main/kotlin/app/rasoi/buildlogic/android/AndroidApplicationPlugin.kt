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
     * Release builds sign with the upload key named by `RASOI_KEYSTORE_*` environment variables (CI) or
     * `rasoi.signing.*` Gradle properties (local builds, kept in `~/.gradle/gradle.properties`); nothing secret is
     * committed.
     * Without them the release is unsigned (ADR 0024). Shrinking stays off until a device-tested set of keep rules
     * exists.
     */
    private fun Project.configureRelease(android: ApplicationExtension) {
        val release = android.buildTypes.getByName("release")
        release.isMinifyEnabled = false
        val keystore = signingSetting("storeFile", "RASOI_KEYSTORE_FILE")?.trim() ?: return
        val storePassword = signingSetting("storePassword", "RASOI_KEYSTORE_PASSWORD")
        val signing = android.signingConfigs.create("release")
        signing.storeFile = file(keystore)
        signing.storePassword = storePassword
        signing.keyAlias = signingSetting("keyAlias", "RASOI_KEY_ALIAS")?.trim()
        // A PKCS12 keystore (keytool's default) protects its key with the store password.
        signing.keyPassword = signingSetting("keyPassword", "RASOI_KEY_PASSWORD") ?: storePassword
        release.signingConfig = signing
    }

    // The environment wins: the self-hosted CI runner shares the owner's ~/.gradle, whose properties must not
    // replace the key CI was given.
    private fun Project.signingSetting(property: String, environment: String): String? =
        providers.environmentVariable(environment)
            .orElse(providers.gradleProperty("rasoi.signing.$property"))
            .orNull
            ?.takeIf { it.isNotBlank() }

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
