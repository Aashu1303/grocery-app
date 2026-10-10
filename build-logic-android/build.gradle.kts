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
