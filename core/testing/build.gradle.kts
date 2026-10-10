plugins { id("rasoi.kmp-library") }

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            api(project(":core:domain"))
            api(kotlin("test"))
            api(libs.kotlinx.coroutines.test)
            api(libs.turbine)
        }
    }
}
