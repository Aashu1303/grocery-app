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
    implementation(libs.plugin.kotlin.serialization)
    implementation(libs.plugin.compose.compiler)
    implementation(libs.plugin.compose)
    implementation(libs.plugin.kover)
    implementation(libs.plugin.detekt)
    implementation(libs.plugin.ktlint)
    implementation(libs.plugin.sqldelight)
    implementation(libs.plugin.roborazzi)
    testImplementation(kotlin("test"))
}

tasks.test { useJUnitPlatform() }

gradlePlugin {
    plugins {
        register("root") {
            id = "rasoi.root"
            implementationClass = "app.rasoi.buildlogic.RootPlugin"
        }
        register("quality") {
            id = "rasoi.quality"
            implementationClass = "app.rasoi.buildlogic.QualityPlugin"
        }
        register("kmpLibrary") {
            id = "rasoi.kmp-library"
            implementationClass = "app.rasoi.buildlogic.KmpLibraryPlugin"
        }
        register("composeLibrary") {
            id = "rasoi.compose-library"
            implementationClass = "app.rasoi.buildlogic.ComposeLibraryPlugin"
        }
        register("feature") {
            id = "rasoi.feature"
            implementationClass = "app.rasoi.buildlogic.FeaturePlugin"
        }
        register("jvmServer") {
            id = "rasoi.jvm-server"
            implementationClass = "app.rasoi.buildlogic.JvmServerPlugin"
        }
    }
}
