plugins {
    id("org.jetbrains.kotlin.jvm")
    `maven-publish` apply true
    signing apply true
    `java-library` apply true
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.maven.apache.org/maven2/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.onarandombox.com/content/groups/public/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://mvn.lumine.io/repository/maven-public/")
    maven("https://www.matteodev.it/spigot/public/maven/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven("https://maven.pkg.github.com/VickyE2/VickyE-s_Utilities") {
        credentials {
            val usernameValue = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
            ?: throw GradleException("Publishing username not set in 'gpr.user' property or 'USERNAME' environment variable")
            username = usernameValue.toString()
            val passwordValue = project.findProperty("gpr.key") ?: System.getenv("TOKEN")
            ?: throw GradleException("Publishing password not set in 'gpr.key' property or 'TOKEN' environment variable")
            password = passwordValue.toString()
        }
    }
    maven("https://nexus.betonquest.org/repository/betonquest/")
    // mavenLocal()
}

dependencies {
    api("net.sf.trove4j:core:3.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.10")
    compileOnly("org.reflections:reflections:0.10.2")
    api("io.github.vickye2:vicky-utils-core:0.0.1")
    api(project(":structure_loader"))
    api("net.sandrohc:schematic4j:1.1.0")
    api("com.google.guava:guava:33.1.0-jre")
    api("org.yaml:snakeyaml:2.4")
    implementationBundled("de.articdive:jnoise:3.0.2")
    implementationBundled("de.articdive:jnoise-pipeline:4.1.0")
    compileOnly("com.dfsek.terra:api:6.5.0-BETA+060cbfd0c")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.1.10")
    compileOnly("org.hibernate.orm:hibernate-core:6.4.1.Final")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
}

group = "org.vicky.vspe"
version = "0.0.1-ARI"
description = "VSPE"

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

publishing {
    publications {
        withType<MavenPublication>().configureEach {
            artifactId = "vspe-core"
        }
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(tasks.named("javadocJar"))
            artifact(tasks.named("sourcesJar"))

            groupId = "io.github.vickye2"
            artifactId = "vspe-core"
            version = project.version.toString()

            pom {
                name.set("Vicky's Survival Plus Essentials")
                description.set(
                    "A must need..."
                )
                inceptionYear.set("2024")
                url.set("https://github.com/VickyE2/VickyEs-Survival-Plus-Essentials")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("vickye")
                        name.set("VickyE2")
                        url.set("https://github.com/VickyE2/")
                        email.set("f.b.cgamingco@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/VickyE2/VickyEs-Survival-Plus-Essentials.git")
                    developerConnection.set("scm:git:ssh://github.com/VickyE2/VickyEs-Survival-Plus-Essentials.git")
                    url.set("https://github.com/VickyE2/VickyEs-Survival-Plus-Essentials")
                }
            }
        }
    }

    repositories {
        mavenLocal()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/VickyE2/VickyEs-Survival-Plus-Essentials")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
            }
        }
        /*maven {
            name = "OSSRH"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = findProperty("ossrhUsername") as String? ?: System.getenv("ossrhUsername")
                password = findProperty("ossrhPassword") as String? ?: System.getenv("ossrhPassword")
            }
        }*/
    }
}

signing {
    useInMemoryPgpKeys(
        project.findProperty("signing.SIGNING_KEY_ID") as String? ?:System.getenv("SIGNING_KEY_ID") as String,
        project.findProperty("signing.SIGNING_KEY") as String? ?:System.getenv("SIGNING_KEY"),
        project.findProperty("signing.SIGNING_PASSWORD") as String? ?:System.getenv("SIGNING_PASSWORD") as String
    )
    sign(the<PublishingExtension>().publications["maven"])
}
