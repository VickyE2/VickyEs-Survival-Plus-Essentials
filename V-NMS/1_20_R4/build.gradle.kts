plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") apply true
    id("io.papermc.paperweight.userdev") apply true
}

group = "org.vicky.vnms"
version = "1_20_R4"

dependencies {
    api(project(":V-NMS", configuration = "shadow"))
    // compileOnly("io.papermc.paper:dev-bundle:1.20.4-R0.1-SNAPSHOT")
    paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT")
    compileOnly("io.github.vickye2:vicky-utils-core:all-0.0.1-BETA")
    compileOnly("io.github.vickye2:vicky-utils-bukkit:1.20.4-0.0.1-BETA")
    compileOnly("io.github.vickye2:vspe-core:0.0.1-ARI")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}


configurations.getByName("api").isCanBeConsumed = false
configurations.getByName("implementation").isCanBeConsumed = false

tasks.named("reobfJar") {}

tasks.jar {
    dependsOn(tasks.named("reobfJar"))
}