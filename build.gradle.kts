
plugins {
    kotlin("multiplatform") version "1.9.22"
}

repositories{
    mavenCentral()
}

kotlin {
    jvm { }
    js {
        IR
        generateTypeScriptDefinitions()
        useEsModules()
        browser()
        nodejs()
        binaries.library()
    }

    sourceSets {
        jvmMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            implementation("com.github.dpaukov:combinatoricslib3:3.3.3")
            // https://mvnrepository.com/artifact/io.github.nsk90/kstatemachine-coroutines-jvm
            implementation("io.github.nsk90:kstatemachine-coroutines-jvm:0.27.0")
            implementation("io.github.nsk90:kstatemachine-jvm:0.27.0")
        }
        jsMain.dependencies {
                // ...
                implementation(npm("js-combinatorics", "2.1.2"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            // https://mvnrepository.com/artifact/com.google.guava/guava
            implementation("org.assertj:assertj-core:3.23.1")
            // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-params
            implementation("org.junit.jupiter:junit-jupiter-params:5.11.0-M1")
        }

    }
}
