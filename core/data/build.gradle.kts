plugins { id("rasoi.kmp-library") }

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:domain"))
            implementation(project(":core:contract"))
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
