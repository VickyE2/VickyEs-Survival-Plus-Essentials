plugins {
    id("org.jetbrains.kotlin.jvm")
    id("io.papermc.paperweight.userdev") apply true
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
    api("org.vicky.vspe:vspe-core:0.0.1-ARI")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.1.10")

    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("org.hibernate.orm:hibernate-core:6.4.1.Final")
    compileOnly("io.github.vickye2:vicky-utils-bukkit:1.20.4-0.0.1-BETA")
    compileOnly("org.betonquest:betonquest:2.2.1")
    compileOnly("me.clip:placeholderapi:2.10.10")
    compileOnly("io.lumine:Mythic-Dist:5.6.1")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.1.0")
    compileOnly("com.github.LoneDev6:api-itemsadder:3.6.1")
    compileOnly("dev.lone:LoneLibs:1.0.58")
    compileOnly("com.onarandombox.multiversecore:Multiverse-Core:4.3.1")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.12")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.9")
    compileOnly("com.onarandombox.multiverseinventories:Multiverse-Inventories:4.2.3")
    compileOnly("com.onarandombox.multiverseportals:Multiverse-Portals:4.2.0")
    compileOnly("com.dfsek.terra:api:6.5.0-BETA+060cbfd0c")
    compileOnly("com.dfsek.terra:bukkit:6.5.0-BETA+060cbfd0c")
    compileOnly("com.dfsek.terra:manifest-addon-loader:1.0.0-BETA+fd6decc70")
    compileOnly("com.github.ZockerAxel:CrazyAdvancementsAPI:v2.1.17a")
    compileOnly("dev.jorel:commandapi-bukkit-core:9.7.0")

    implementation(project(":V-NMS"))
    api(project(":structure_loader"))

    paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT")
}

group = "org.vicky.vicky.vickyes-survival-plus-essentials"
version = "0.0.1-ARI"
description = "VSPE"

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}