plugins {
    id("org.jetbrains.kotlin.jvm")
    id("io.papermc.paperweight.userdev") apply true
    id("io.github.goooler.shadow") version "8.1.8" apply true
}

dependencies {
    implementation("net.sf.trove4j:core:3.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.reflections:reflections:0.10.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.10")
    api("io.github.vickye2:vspe-core:0.0.1-ARI") {
        exclude("io.github.vickye2", "vicky-utils-core")
    }

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.1.10")

    compileOnly("org.hibernate.orm:hibernate-core:6.4.1.Final")
    compileOnly("io.github.vickye2:vicky-utils-bukkit:1.20.4-0.0.1-BETA")
    compileOnly("org.betonquest:betonquest:2.2.1")
    compileOnly("me.clip:placeholderapi:2.10.10")
    compileOnly("io.lumine:Mythic-Dist:5.6.1")
    compileOnly("com.comphenix.protocol:ProtocolLib:3.6.5")
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

    implementation(project(":V-NMS:1_20_R4")) {
        exclude("io.github.vickye2", "vicky-utils-core")
    }
    implementation(project(":structure_loader")) {
        exclude("io.github.vickye2", "vicky-utils-core")
    }

    paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT")
}

group = "org.vicky.vicky.vickyes-survival-plus-essentials"
version = "0.0.1-ARI"
description = "VSPE"

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.jar {
    enabled = false
    dependsOn(":structure_loader:jar")
    dependsOn(":V-NMS:reobfJar")
    dependsOn(":V-NMS:1_20_R4:reobfJar")

    from({
        project(":V-NMS").tasks.named("reobfJar").get().outputs.files.map { zipTree(it) }
    })
    from({
        project(":V-NMS:1_20_R4").tasks.named("reobfJar").get().outputs.files.map { zipTree(it) }
    })

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude("org/hibernate/**")
    exclude("org/jboss/logging/**")
    exclude("jakarta/persistence/**")
    exclude("javax/**")
    exclude("org/antlr/**")
    exclude("org/glassfish/**")
    exclude("org/vicky/utilities/**")
    exclude("org/vicky/shaded/**")
    exclude("org/vicky/platform/**")

    dependencies {
        exclude(("io.github.vickye2:vicky-utils-core:all-0.0.1-BETA"))
    }

    from({
        (configurations.runtimeClasspath.get())
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}
tasks.shadowJar {
    archiveClassifier.set("dev")
    dependsOn(":structure_loader:jar")
    dependsOn(":V-NMS:reobfJar")
    dependsOn(":V-NMS:1_20_R4:reobfJar")

    from({
        project(":V-NMS").tasks.named("reobfJar").get().outputs.files.map { zipTree(it) }
    })
    from({
        project(":V-NMS:1_20_R4").tasks.named("reobfJar").get().outputs.files.map { zipTree(it) }
    })

    exclude("org/hibernate/**")
    exclude("org/jboss/logging/**")
    exclude("jakarta/persistence/**")
    exclude("javax/**")
    exclude("org/antlr/**")
    exclude("org/glassfish/**")
    exclude("org/vicky/utilities/**")
    exclude("org/vicky/shaded/**")
    exclude("org/vicky/platform/**")

    dependencies {
        exclude(dependency("io.github.vickye2:vicky-utils-core:all-0.0.1-BETA"))
    }
}

tasks.named("build") {
    dependsOn("reobfJar")
}

configurations.all {
    exclude(group = "io.github.vickye2", module = "vicky-utils-core")
}