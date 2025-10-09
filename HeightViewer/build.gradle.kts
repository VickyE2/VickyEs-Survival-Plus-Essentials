plugins {
    `java-library`
    id("org.openjfx.javafxplugin") version "0.0.14"
    kotlin("jvm") version "2.1.10"
}

group = "org.vicky.vicky_utils"
version = "unspecified"

repositories {
    mavenCentral()
}

javafx {
    version = "20" // Replace with the desired JavaFX version
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.graphics", "javafx.swing") // Add other modules as needed
}

dependencies {
    api(project(":VSPE")) // comes from here
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.openjfx:javafx-controls:20")
    implementation("org.openjfx:javafx-graphics:20")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
}

tasks.test {
    useJUnitPlatform()
}