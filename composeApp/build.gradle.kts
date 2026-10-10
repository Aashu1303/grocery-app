plugins {
    id("rasoi.compose-application")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:designsystem"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.resources)
            implementation(libs.navigation.compose)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.koin.test)
        }
        findByName("androidMain")?.dependencies {
            implementation(libs.androidx.activity.compose)
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "app.rasoi.app.resources"
    generateResClass = always
}
