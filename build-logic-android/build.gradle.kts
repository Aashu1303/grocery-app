plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(file("../config/detekt/detekt.yml"))
}

group = "app.rasoi"

kotlin {
    compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17 }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.plugin.kotlin)
    implementation(libs.plugin.agp)
    implementation(libs.plugin.compose.compiler)
    // On the root buildscript classpath with the other Android plugins, so it resolves only when Google Maven is in use.
    implementation(libs.plugin.google.services)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "rasoi.android-library"
            implementationClass = "app.rasoi.buildlogic.android.AndroidLibraryPlugin"
        }
        register("androidApplication") {
            id = "rasoi.android-application"
            implementationClass = "app.rasoi.buildlogic.android.AndroidApplicationPlugin"
        }
    }
}
