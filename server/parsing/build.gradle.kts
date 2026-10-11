plugins { id("rasoi.jvm-server") }

dependencies {
    api(project(":core:model"))
    implementation(libs.pdfbox)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test>().configureEach {
    val fixtures = rootProject.file("fixtures/invoices")
    // Only redacted fixtures and snapshots are inputs; the git-ignored PDFs beside them must not affect caching.
    inputs.files(fileTree(fixtures) { include("**/*.redacted.txt", "**/*.expected.json") })
        .withPropertyName("invoiceFixtures")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    systemProperty("rasoi.fixtures", fixtures.absolutePath)
}

tasks.register<JavaExec>("extractFixture") {
    group = "verification"
    description = "Writes a redacted positioned-text fixture from a local invoice PDF (see add-invoice-parser skill)."
    classpath = sourceSets["test"].runtimeClasspath
    mainClass = "app.rasoi.server.parsing.tools.ExtractFixtureMainKt"
    workingDir = rootDir
}
