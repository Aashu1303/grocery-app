plugins {
    id("rasoi.android-application")
    id("rasoi.quality")
    id("com.google.gms.google-services")
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
    // Firebase SDK available in all builds; sign-in provider is source-set-selected (ADR 0024 updated).
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    releaseImplementation(libs.kotlinx.coroutines.core)
}
android {
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}
