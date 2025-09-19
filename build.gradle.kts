import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.2.0"
    `maven-publish`
}

group = "net.botwithus.kxapi"
version = "0.1-SNAPSHOT"

kotlin {
    jvmToolchain(24)
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_24)
    }
}

tasks.jar {
    manifest {
        attributes(
            "Automatic-Module-Name" to "kxapi"
        )
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://nexus.botwithus.net/repository/maven-snapshots/")
}

dependencies {
    implementation("net.botwithus.api:api:1.+")
    implementation("net.botwithus.imgui:imgui:1.+")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("net.botwithus.xapi:xapi:2.0.+")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
}

publishing {
    repositories {
        mavenLocal()

        val repoUser = System.getenv("MAVEN_REPO_USER")?.takeIf { it.isNotBlank() }
        val repoPass = System.getenv("MAVEN_REPO_PASS")?.takeIf { it.isNotBlank() }

        if (repoUser != null && repoPass != null) {
            maven {
                url = if (version.toString().endsWith("SNAPSHOT")) {
                    uri("https://nexus.botwithus.net/repository/maven-snapshots/")
                } else {
                    uri("https://nexus.botwithus.net/repository/maven-releases/")
                }
                credentials {
                    username = repoUser
                    password = repoPass
                }
            }
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "kxapi"

            pom {
                name.set("BotWithUs KXAPI")
                description.set("Extended Kotlin API framework for BotWithUs RuneScape 3 bot development")
                properties.set(mapOf("maven.compiler.source" to "24", "maven.compiler.target" to "24"))
            }
        }
    }
}
