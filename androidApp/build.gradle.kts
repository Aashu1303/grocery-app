plugins {
    id("rasoi.android-application")
    id("rasoi.quality")
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
    // Only release builds carry a production sign-in provider; debug builds use the development one (ADR 0024).
    releaseImplementation(libs.firebase.auth)
    releaseImplementation(libs.kotlinx.coroutines.core)
}
