import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// name = "IPv4-Count"
version = "2.0-SNAPSHOT"
description = "Count unique IP addresses listed in a text file"

plugins {
    kotlin("jvm") version "1.9.24"
    id("me.champeau.jmh") version "0.7.2"
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
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
    testImplementation("org.mockito.kotlin", "mockito-kotlin", "5.3.1")
}

tasks.test {
    maxHeapSize = "1024M"
    useJUnitPlatform()
}
