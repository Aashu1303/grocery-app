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
}

tasks.named("check") {
    dependsOn(gradle.includedBuild("build-logic").task(":check"))
    if (providers.gradleProperty("rasoi.android").orNull.toBoolean()) {
        dependsOn(gradle.includedBuild("build-logic-android").task(":check"))
    }
}
