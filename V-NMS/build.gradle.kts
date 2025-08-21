plugins {
    id("java") apply true
    id("org.jetbrains.kotlin.jvm") apply true
    id("io.papermc.paperweight.userdev") apply true
}

group = "org.vicky"
version = "1.0-SNAPSHOT"

configurations.getByName("api").isCanBeConsumed = false
configurations.getByName("implementation").isCanBeConsumed = false

tasks.named("reobfJar") {
    // no-op; just to ensure task exists before we publish it
}

// Optional, but helps downstream ordering
tasks.jar {
    dependsOn(tasks.named("reobfJar"))
}

dependencies {
    paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT")
    // api("com.cjcrafter:mechanicscore:3.4.1")
    api("net.kyori:adventure-platform-bukkit:4.4.1")
    api("net.kyori:adventure-text-serializer-legacy:4.15.0")
    api("net.kyori:adventure-api:4.15.0")
    api("xyz.jpenilla:reflection-remapper:0.1.0")
    implementation("com.github.cryptomorin:XSeries:13.3.3")
    compileOnly("com.comphenix.protocol:ProtocolLib:3.6.5")
    compileOnly("io.github.vickye2:vicky-utils-core:all-0.0.1-BETA")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}