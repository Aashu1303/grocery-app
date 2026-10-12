plugins { id("rasoi.kmp-library") }

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:domain"))
            implementation(project(":core:contract"))
            implementation(libs.kotlinx.coroutines.core)
            // HttpClient is part of RasoiApi and HttpClientFactory signatures, so consumers need it on their classpath.
            api(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
        }
        commonTest.dependencies {
            implementation(project(":core:testing"))
            implementation(libs.ktor.client.mock)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        // Platform source sets exist only when their targets are enabled (ADR 0007), so configure them lazily.
        matching { it.name == "androidMain" }.configureEach {
            dependencies {
                implementation(libs.ktor.client.okhttp)
                implementation(libs.datastore.preferences)
            }
        }
        matching { it.name == "iosMain" }.configureEach {
            dependencies {
                implementation(libs.ktor.client.darwin)
                implementation(libs.multiplatform.settings)
            }
        }
    }
}
