plugins { id("rasoi.kmp-library") }

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            implementation(libs.kotlinx.coroutines.core)
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
