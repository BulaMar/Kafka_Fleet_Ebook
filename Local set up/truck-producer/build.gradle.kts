plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "pl.jlabs.ebook"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.kafka:kafka-clients:3.6.1")

    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("org.slf4j:slf4j-simple:2.0.9")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "pl.jlabs.ebook.Main"
    }
}

tasks.test {
    useJUnitPlatform()
}
