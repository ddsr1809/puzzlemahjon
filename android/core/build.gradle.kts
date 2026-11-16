import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // Las pruebas solo usan kotlin.test. Se ejecutan con JUnit 4, que Gradle 9 sigue
    // soportando sin declarar el lanzador de JUnit Platform.
    testImplementation(libs.kotlin.test.junit)
}

tasks.test {
    useJUnit()
    testLogging {
        events("failed", "skipped")
        showStandardStreams = false
    }
}
