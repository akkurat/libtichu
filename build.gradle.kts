plugins {
    kotlin("jvm")
    `java-library`
}


repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")
    implementation("com.github.dpaukov:combinatoricslib3:3.3.3")
    implementation(kotlin("test"))
    // https://mvnrepository.com/artifact/com.google.guava/guava
    testImplementation("org.assertj:assertj-core:3.23.1")
    // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-params
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.0-M1")

}
