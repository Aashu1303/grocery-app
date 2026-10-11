plugins { id("rasoi.kmp-library") }

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(project(":core:testing"))
        }
    }
}

kover {
    reports {
        verify {
            rule { minBound(90) }
        }
    }
}
