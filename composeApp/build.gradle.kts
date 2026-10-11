plugins {
    id("rasoi.compose-application")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:data"))
            implementation(project(":core:model"))
            implementation(project(":core:domain"))
            implementation(project(":core:designsystem"))
            implementation(project(":feature:household"))
            implementation(project(":feature:invoiceimport"))
            implementation(project(":feature:pantry"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.resources)
            implementation(libs.navigation.compose)
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(project(":core:testing"))
            implementation(libs.koin.test)
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "app.rasoi.app.resources"
    generateResClass = always
}
