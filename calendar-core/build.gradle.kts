plugins {
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// Regenerates src/main/resources/corrections.json from the golden CSVs listed in coverage.json.
tasks.register<JavaExec>("generateCorrections") {
    group = "verification"
    description = "Diff raw engine vs golden and rewrite corrections.json"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("id.kalen3r.core.CorrectionsGenerator")
    args(layout.projectDirectory.file("src/main/resources/corrections.json").asFile.absolutePath)
}
