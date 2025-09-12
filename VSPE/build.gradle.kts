subprojects {
    group = "org.vicky.vickyes-survival-plus-essentials"
    description = "VSPE"

    repositories {
        mavenLocal()
    }

    dependencies {
        implementation("org.reflections:reflections:0.10.2")
        api("io.github.vickye2:vspe-core:0.0.1-ARI") {
            exclude(group = "io.github.vickye2", module = "vspe-core")
            exclude(group = "io.github.vickye2", module = "vicky-utils-core")
            exclude(group = "org.vicky.vspe.addon", module = "structure_loader")
            exclude(group = "de.articdive", module = "jnoise")
            exclude(group = "de.articdive", module = "jnoise-pipeline")
        }
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
        testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.0")
        testImplementation("org.jetbrains.kotlin:kotlin-test:2.1.10")
        compileOnly("org.hibernate.orm:hibernate-core:6.4.1.Final")
    }

    tasks.named("build") {
        dependsOn("reobfJar")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    tasks.withType<Javadoc> {
        options.encoding = "UTF-8"
    }
}