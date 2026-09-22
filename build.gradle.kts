import java.util.Locale

plugins {
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.gradle.versions)
    alias(kotlinx.plugins.serialization) apply false
    alias(libs.plugins.aboutlibraries) apply false
    alias(libs.plugins.aboutlibraries.android) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.moko) apply false
    alias(libs.plugins.sqldelight) apply false
}

tasks.named("dependencyUpdates", com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask::class.java).configure {
    rejectVersionIf {
        val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { candidate.version.uppercase(Locale.ROOT).contains(it) }
        val regex = "^[0-9,.v-]+(-r)?$".toRegex()
        val isStable = stableKeyword || regex.matches(candidate.version)
        isStable.not()
    }
    // optional parameters
    checkForGradleUpdate = true
    outputFormatter = "json"
    outputDir = "build/dependencyUpdates"
    reportfileName = "report"
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

configurations.all {
    resolutionStrategy {
        // Force Kotlin 2.4.10 to override Compose BOM 2026.02.00 constraints
        force("org.jetbrains.kotlin:kotlin-stdlib:2.4.10")
        force("org.jetbrains.kotlin:kotlin-stdlib-common:2.4.10")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.4.10")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.4.10")
        force("org.jetbrains.kotlin:kotlin-reflect:2.4.10")
        force("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.4.10")
        force("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:2.4.10")
        force("org.jetbrains.kotlin:kotlin-scripting-jvm:2.4.10")
        force("org.jetbrains.kotlin:kotlin-scripting-jvm-host:2.4.10")
    }
}
