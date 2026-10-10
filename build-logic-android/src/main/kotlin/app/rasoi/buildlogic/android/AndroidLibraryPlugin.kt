package app.rasoi.buildlogic.android

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            addAndroidTarget()
            extensions.getByType(LibraryExtension::class.java).apply {
                namespace = androidNamespace()
                compileSdk = AndroidConfig.COMPILE_SDK
                defaultConfig.minSdk = AndroidConfig.MIN_SDK
                compileOptions.sourceCompatibility = JavaVersion.VERSION_17
                compileOptions.targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
}
