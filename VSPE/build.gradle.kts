subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "io.papermc.paperweight.userdev")
    apply(plugin = "io.github.goooler.shadow")
    apply(plugin = "io.papermc.paperweight.userdev")

    group = "org.vicky.vicky.vickyes-survival-plus-essentials"
    description = "VSPE"
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
        compileOnly("org.betonquest:betonquest:2.2.1")
        compileOnly("me.clip:placeholderapi:2.10.10")
        compileOnly("io.lumine:Mythic-Dist:5.6.1")
        compileOnly("com.comphenix.protocol:ProtocolLib:3.6.5")
        compileOnly("com.github.LoneDev6:api-itemsadder:3.6.1")
        compileOnly("dev.lone:LoneLibs:1.0.58")
        compileOnly("com.github.ZockerAxel:CrazyAdvancementsAPI:v2.1.17a")
        compileOnly("dev.jorel:commandapi-bukkit-core:9.7.0")
    }

    tasks.named("build") {
        dependsOn("reobfJar")
    }

    configurations.all {
        exclude(group = "io.github.vickye2", module = "vicky-utils-core")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    tasks.withType<Javadoc> {
        options.encoding = "UTF-8"
    }
}