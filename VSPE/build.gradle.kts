plugins {
    id("org.jetbrains.kotlin.jvm")
    // id("io.papermc.paperweight.userdev") apply true
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
            username = (project.findProperty("gpr.user") ?: System.getenv("USERNAME_GITHUB")).toString()
            password = (project.findProperty("gpr.key") ?: System.getenv("TOKEN_GITHUB")).toString()
        }
    }
    maven("https://nexus.betonquest.org/repository/betonquest/")
    // mavenLocal()
}

dependencies {
    api("net.sf.trove4j:core:3.1.0")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    api("org.reflections:reflections:0.10.2")
    api("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.10")
    implementation("net.sandrohc:schematic4j:1.1.0")
    implementation(files("libs/jNBT-1.6.0.jar"))
    implementation("com.google.guava:guava:33.1.0-jre")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.1.10")

    compileOnly("org.hibernate.orm:hibernate-core:6.4.1.Final")
    implementation("io.github.vickye2:vicky-utils-core:all-0.0.1-BETA")
    // compileOnly("com.dfsek.terra:api:6.5.0-BETA+060cbfd0c")
    // compileOnly("com.dfsek.terra:manifest-addon-loader:1.0.0-BETA+fd6decc70")

    api(project(":structure_loader"))
}

group = "org.vicky.vicky.vickyes-survival-plus-essentials-core"
version = "0.0.1-ARI"
description = "VSPE"

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}