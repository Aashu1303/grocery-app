plugins { id("rasoi.jvm-server") }

dependencies {
    api(project(":core:model"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.java.jwt)
    testImplementation(libs.ktor.client.mock)
}
