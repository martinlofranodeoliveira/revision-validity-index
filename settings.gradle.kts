// Copyright 2026 Lofrano Tecnologia. MIT licensed.
import org.gradle.api.artifacts.verification.DependencyVerificationMode
import org.gradle.util.GradleVersion

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "revision-validity-index"

check(GradleVersion.current().version == "8.10.2") {
    "This development version requires Gradle 8.10.2."
}
check(JavaVersion.current() == JavaVersion.VERSION_21) {
    "This development version requires JDK 21."
}

gradle.startParameter.dependencyVerificationMode = DependencyVerificationMode.STRICT
val prepareIntegrity = providers.gradleProperty("prepareIntegrity").orNull == "true"
if (prepareIntegrity) {
    check(System.getenv("CI").isNullOrBlank()) {
        "Integrity preparation is not permitted in CI."
    }
    check(gradle.startParameter.isWriteDependencyLocks) {
        "Integrity preparation requires --write-locks."
    }
    check("sha256" in gradle.startParameter.writeDependencyVerifications) {
        "Integrity preparation requires --write-verification-metadata sha256."
    }
} else {
    check(file("gradle.lockfile").isFile) {
        "Missing source integrity record: gradle.lockfile. See docs/integrity.md."
    }
    check(file("gradle/verification-metadata.xml").isFile) {
        "Missing source integrity record: gradle/verification-metadata.xml. See docs/integrity.md."
    }
}

// Keep project state beside the other ignored build outputs.
gradle.startParameter.projectCacheDir = file("build/project-cache")
val compilerState = file("build/kotlin-state")
check(compilerState.resolve("sessions").let { it.isDirectory || it.mkdirs() }) {
    "Could not create the project-local Kotlin compiler session directory."
}
gradle.startParameter.setProjectProperties(
    gradle.startParameter.projectProperties +
        mapOf("kotlin.project.persistent.dir" to compilerState.absolutePath)
)
