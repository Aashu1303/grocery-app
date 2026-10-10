package app.rasoi.buildlogic.android

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            addAndroidTarget()
            extensions.getByType(ApplicationExtension::class.java).apply {
                namespace = "app.rasoi"
                compileSdk = AndroidConfig.COMPILE_SDK
                defaultConfig.apply {
                    applicationId = "app.rasoi"
                    minSdk = AndroidConfig.MIN_SDK
                    targetSdk = AndroidConfig.TARGET_SDK
                    versionCode = 1
                    versionName = "0.1.0"
                }
                compileOptions.sourceCompatibility = JavaVersion.VERSION_17
                compileOptions.targetCompatibility = JavaVersion.VERSION_17
                buildTypes.getByName("release").isMinifyEnabled = false
            }
        }
    }
}
