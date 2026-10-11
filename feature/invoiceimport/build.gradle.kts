plugins { id("rasoi.feature") }

kotlin {
    sourceSets {
        // The Android source set exists only when the Android target is enabled (ADR 0007), so configure it lazily.
        matching { it.name == "androidMain" }.configureEach {
            dependencies { implementation(libs.androidx.activity.compose) }
        }
    }
}
