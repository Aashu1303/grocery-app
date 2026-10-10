plugins {
    id("rasoi.android-application")
    id("rasoi.quality")
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
}
