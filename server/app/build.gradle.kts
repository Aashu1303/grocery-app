plugins {
    id("rasoi.jvm-server")
    application
}

application {
    mainClass = "app.rasoi.server.ApplicationKt"
}

dependencies {
    implementation(project(":core:contract"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization.json)
    implementation(libs.logback)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
}
