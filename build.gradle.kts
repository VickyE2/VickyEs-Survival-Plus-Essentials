import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin
val javaVersion = 21

plugins {
    // Add plugins here that you *don't* want to auto-apply
    kotlin("jvm") version "2.1.10" apply false
    id("io.papermc.paperweight.userdev") version "1.7.7" apply false
    `java-library`
    `maven-publish`
    id("io.github.goooler.shadow") version "8.1.8" apply true
}

allprojects {
    repositories {
        maven("https://maven.pkg.github.com/VickyE2/VickyE-s_Utilities") {
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "io.github.goooler.shadow")

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    configurations.all {
        resolutionStrategy {
            force("com.google.guava:guava:32.1.3-jre") // or latest that works for all
        }
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }

    plugins.withType<KotlinPlatformJvmPlugin> {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            kotlinOptions {
                jvmTarget = "$javaVersion"
            }
        }
    }

    tasks.shadowJar {
        archiveClassifier.set("")
    }

    tasks.named<ShadowJar>("shadowJar") {
        archiveBaseName.set("VSPE")
        version = project.version
        configurations = listOf(
            project.configurations.runtimeClasspath.get(),
        )
        archiveClassifier.set("")
        mergeServiceFiles()
        minimize()
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "org.vicky" // or whatever you want
            artifactId = "nms"
            version = "1.0.0"

            from(components["java"])
        }
    }
    repositories {
        mavenLocal()
    }
}