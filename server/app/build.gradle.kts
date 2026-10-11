plugins {
    id("rasoi.jvm-server")
    application
}

application {
    mainClass = "app.rasoi.server.ApplicationKt"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:contract"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.serialization.json)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgres)
    implementation(libs.postgres.driver)
    implementation(libs.hikari)
    implementation(libs.java.jwt)
    implementation(libs.jwks.rsa)
    implementation(libs.logback)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.testcontainers.postgres)
    testImplementation(libs.ktor.client.okhttp)
}

tasks.register<JavaExec>("seedDemo") {
    group = "application"
    description = "Seeds the demo household into a running dev server (RASOI_DEMO_URL)."
    classpath = sourceSets["test"].runtimeClasspath
    mainClass = "app.rasoi.server.demo.DemoSeedMainKt"
}
