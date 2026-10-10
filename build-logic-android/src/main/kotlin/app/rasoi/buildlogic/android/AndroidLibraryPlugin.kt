package app.rasoi.buildlogic.android

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** Adds the Android target to a KMP module with AGP's Kotlin Multiplatform library plugin (ADR 0013). */
class AndroidLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.kotlin.multiplatform.library")
            val android = extensions.getByType(KotlinMultiplatformExtension::class.java)
                .extensions.getByType(KotlinMultiplatformAndroidLibraryTarget::class.java)
            android.namespace = androidNamespace()
            android.compileSdk = AndroidConfig.COMPILE_SDK
            android.minSdk = AndroidConfig.MIN_SDK
            android.androidResources.enable = true
        }
    }
}
