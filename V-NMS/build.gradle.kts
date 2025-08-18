plugins {
    id("org.jetbrains.kotlin.jvm") apply true
    id("io.papermc.paperweight.userdev") apply true
}

group = "org.vicky"
version = "1.0-SNAPSHOT"


dependencies {
    paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT")
    api("com.cjcrafter:mechanicscore:3.4.1")
    compileOnly("com.comphenix.protocol:ProtocolLib:3.6.5")
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    api("net.kyori:adventure-platform-bukkit:4.4.1")
    api("net.kyori:adventure-text-serializer-legacy:4.15.0")
    api("net.kyori:adventure-api:4.15.0")
    api("io.github.vickye2:vicky-utils-core:all-0.0.1-BETA")
}

tasks.test {
    useJUnitPlatform()
}