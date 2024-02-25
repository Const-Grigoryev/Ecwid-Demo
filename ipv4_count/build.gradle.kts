import org.gradle.api.JavaVersion

// name = "IPv4-count"
// version = "0.1"
// description = "..."

plugins {
    kotlin("jvm") version "1.6.21"
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_16
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter", "junit-jupiter", "5.8.1")
    testImplementation("org.mockito", "mockito-core", "3.+")
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}
