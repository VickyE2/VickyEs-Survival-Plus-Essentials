import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin
val javaVersion = 21

plugins {
    // Add plugins here that you *don't* want to auto-apply
    kotlin("jvm") version "2.1.10" apply false
    id("io.papermc.paperweight.userdev") version "1.7.7" apply false
    `java-library`
    `maven-publish`
    id("io.github.goooler.shadow") version "8.1.8" apply false
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://mvn.lumine.io/repository/maven-public/")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.comphenix.net/content/groups/public/")
        maven("https://jitpack.io")
        maven("https://repo.maven.apache.org/maven2/")
        maven("https://repo.onarandombox.com/content/groups/public/")
        maven("https://repo.codemc.io/repository/maven-public/")
        maven("https://oss.sonatype.org/content/groups/public/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://www.matteodev.it/spigot/public/maven/")
        maven("https://repo.codemc.io/repository/maven-snapshots/")
        maven("https://maven.enginehub.org/repo/")
        maven("https://repo.codemc.org/repository/maven-public/")
        maven("https://nexus.betonquest.org/repository/betonquest/")
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
            name = "sonatype-oss-snapshots"
        }
        maven("https://repo.velocitypowered.com/snapshots/") {
            name = "velocity-snapshots"
        }
        maven("https://repo.spongepowered.org/repository/maven-snapshots/") {
            name = "sponge-snapshots"
        }
        maven("https://repo.minebench.de/") {
            name = "minebench-snapshots"
        }
        maven("https://maven.pkg.github.com/VickyE2/VickyE-s_Utilities") {
            credentials {
                username = (project.findProperty("gpr.user") ?: System.getenv("USERNAME_GITHUB")).toString()
                password = (project.findProperty("gpr.key") ?: System.getenv("TOKEN_GITHUB")).toString()
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