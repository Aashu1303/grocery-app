plugins { id("rasoi.compose-library") }

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
            implementation(libs.compose.ui.test.junit4)
            implementation(libs.compose.desktop.linux)
            implementation(libs.roborazzi.compose.desktop)
            implementation(libs.junit4)
        }
    }
}
