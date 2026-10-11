plugins {
    id("rasoi.compose-library")
    id("io.github.takahirom.roborazzi")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.ui)
            api(libs.compose.material3)
            implementation(libs.compose.resources)
            implementation(libs.compose.ui.tooling.preview)
        }
        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(libs.compose.ui.test.junit4)
            implementation(compose.desktop.currentOs)
            implementation(libs.roborazzi.compose.desktop)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.junit4)
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "app.rasoi.core.designsystem.resources"
    generateResClass = always
}

// Golden images are a gate: an unrecorded visual change fails `check` (record with "[record screenshots]").
tasks.named("check") { dependsOn("verifyRoborazziJvm") }
