buildscript {
    if (providers.gradleProperty("rasoi.android").orNull.toBoolean()) {
        repositories {
            google()
            mavenCentral()
            gradlePluginPortal()
        }
        dependencies { classpath("app.rasoi:build-logic-android") }
    }
}

plugins {
    id("rasoi.root")
    id("com.google.gms.google-services") version "4.5.0" apply false
}

tasks.named("check") {
    dependsOn(gradle.includedBuild("build-logic").task(":check"))
    if (providers.gradleProperty("rasoi.android").orNull.toBoolean()) {
        dependsOn(gradle.includedBuild("build-logic-android").task(":check"))
    }
}
