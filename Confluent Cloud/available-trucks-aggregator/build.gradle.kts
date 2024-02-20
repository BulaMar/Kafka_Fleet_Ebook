plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
}

group = "pl.jlabs.ebook"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url= uri("https://packages.confluent.io/maven")
    }
}

dependencies {
    implementation("org.apache.kafka:kafka-clients:3.6.1")
    implementation("org.apache.kafka:kafka-streams:3.6.1")
    implementation("io.confluent:kafka-avro-serializer:7.5.0")
    implementation("io.confluent:kafka-streams-avro-serde:5.2.1")
    implementation("org.apache.avro:avro:1.11.3")

    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("org.slf4j:slf4j-simple:2.0.9")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.apache.kafka:kafka-streams-test-utils:3.6.1")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "pl.jlabs.ebook.Main"
        attributes["Class-Path"] = configurations.compileClasspath.get().joinToString(separator = ",") { it.getName() }
    }
}

tasks.test {
    useJUnitPlatform()
}

avro {
    stringType.set("String")
}
