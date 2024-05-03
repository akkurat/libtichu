import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    application

}

group = "org.taburett"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.github.dpaukov:combinatoricslib3:3.3.3")
    // https://mvnrepository.com/artifact/io.github.nsk90/kstatemachine-coroutines-jvm
    implementation("io.github.nsk90:kstatemachine-coroutines-jvm:0.27.0")
    implementation("io.github.nsk90:kstatemachine-jvm:0.27.0")


    testImplementation(kotlin("test"))
    // https://mvnrepository.com/artifact/com.google.guava/guava
    testImplementation("org.assertj:assertj-core:3.23.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}