import org.gradle.api.JavaVersion

// name = "IPv4-count"
// version = "0.1"
// description = "..."

plugins {
    kotlin("jvm") version "1.6.21"
    id("me.champeau.jmh") version "0.7.2"
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_16
}

application {
    mainClass.set("dev.aspid812.ipv4_count.IPv4CountApp")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter", "junit-jupiter", "5.8.1")
    testImplementation("org.mockito", "mockito-core", "3.+")
}

tasks.test {
    maxHeapSize = "1024M"
    useJUnitPlatform()
}
