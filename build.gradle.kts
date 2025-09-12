import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin
val javaVersion = 21

plugins {
    // Add plugins here that you *don't* want to auto-apply
    kotlin("jvm") version "2.1.10" apply false
    id("io.papermc.paperweight.userdev") version "1.7.7" apply false
    `java-library`
    `maven-publish`
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

    // only configure Java/Forge style projects where sourceSets exist
    plugins.withId("java") {
        // ensure bundled exists if a subproject forgets (buildSrc helper will create too)
        configurations.create("bundled").apply {
            isCanBeConsumed = false
            isCanBeResolved = true
        }

        tasks.jar {
            from({
                configurations["bundled"].resolve()
                    .filter { it.name.endsWith(".jar") }
                    .map { zipTree(it) }
            })
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }

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
}