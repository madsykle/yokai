plugins {
    `kotlin-dsl`
}

dependencies {
    // javapoet 1.13.0 as required by dagger/hilt AGP plugin (2.56.2 and 2.60.1)
    // (see google/dagger#3068 / google/dagger#4976); projects using buildSrc are affected
    implementation("com.squareup:javapoet:1.13.0")
    implementation(androidx.gradle)
    implementation(kotlinx.gradle)
    implementation(kotlinx.compose.compiler.gradle)
    implementation(gradleApi())

    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(files(androidx.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(files(compose.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(files(kotlinx.javaClass.superclass.protectionDomain.codeSource.location))
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
}
