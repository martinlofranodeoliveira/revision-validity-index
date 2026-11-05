// Copyright 2026 Lofrano Tecnologia. MIT licensed.
import org.gradle.api.artifacts.dsl.LockMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.0.21"
}

group = "br.com.lofranotecnologia"
version = "0.1.0"
description = "Bounded in-memory effective intervals with immutable knowledge revisions"

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        allWarningsAsErrors.set(true)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}

dependencyLocking {
    lockAllConfigurations()
    lockMode.set(LockMode.STRICT)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
    maxParallelForks = 1
    maxHeapSize = "192m"
    jvmArgs("-XX:MaxMetaspaceSize=96m", "-XX:ReservedCodeCacheSize=32m")
    systemProperty("file.encoding", "UTF-8")
    testLogging {
        events("failed", "skipped")
        showStandardStreams = false
    }
}

tasks.register<JavaExec>("demoAssert") {
    group = "verification"
    description = "Run the deterministic demo and its internal contract checks directly."
    dependsOn(tasks.classes)
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("br.com.lofranotecnologia.revision.Demo")
    maxHeapSize = "192m"
    jvmArgs("-XX:MaxMetaspaceSize=96m", "-XX:ReservedCodeCacheSize=32m")
}

tasks.register("resolveDependencies") {
    group = "verification"
    description = "Resolve project configurations; does not itself write integrity records."
    dependsOn(tasks.named("testClasses"))
    doLast {
        listOf("compileClasspath", "runtimeClasspath", "testCompileClasspath", "testRuntimeClasspath")
            .forEach { configurations.getByName(it).resolve() }
    }
}
