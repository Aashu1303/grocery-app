plugins {
    id("rasoi.android-application")
    id("rasoi.quality")
}

// google-services.json is git-ignored, so CI and fresh clones build without it; sign-in then reports that Firebase
// is not configured (PhoneAuth.kt) instead of the build failing.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
    // Firebase SDK available in all builds; sign-in provider is source-set-selected (ADR 0024 updated).
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    releaseImplementation(libs.kotlinx.coroutines.core)
}
