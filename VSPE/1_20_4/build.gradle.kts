dependencies {
    compileOnly("io.github.vickye2:vicky-utils-bukkit:1.20.4-0.0.1-BETA")
    compileOnly("com.onarandombox.multiversecore:Multiverse-Core:4.3.1")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.12")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.9")
    compileOnly("com.onarandombox.multiverseinventories:Multiverse-Inventories:4.2.3")
    compileOnly("com.onarandombox.multiverseportals:Multiverse-Portals:4.2.0")
    compileOnly("com.dfsek.terra:api:6.5.0-BETA+060cbfd0c")
    compileOnly("com.dfsek.terra:bukkit:6.5.0-BETA+060cbfd0c")
    compileOnly("com.dfsek.terra:manifest-addon-loader:1.0.0-BETA+fd6decc70")

    implementation(project(":V-NMS:1_20_R4")) {
        exclude("io.github.vickye2", "vicky-utils-core")
    }
    implementation(project(":structure_loader")) {
        exclude("io.github.vickye2", "vicky-utils-core")
    }

    paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT")
}

version = "1.20.4-0.0.1-ARI"

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