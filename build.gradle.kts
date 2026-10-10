plugins {
    id("rasoi.root")
}

tasks.named("check") {
    dependsOn(gradle.includedBuild("build-logic").task(":check"))
}
