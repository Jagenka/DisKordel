plugins {
    id("fabric-loom")
    val kotlinVersion: String by System.getProperties()
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("com.github.johnrengelman.shadow") version "7.1.2"
}
base {
    val archivesBaseName: String by project
    archivesName.set(archivesBaseName)
}
val modVersion: String by project
version = modVersion
val mavenGroup: String by project
group = mavenGroup
repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}
dependencies {
    val minecraft_version: String by project
    minecraft("com.mojang:minecraft:$minecraft_version")
    val yarn_mappings: String by project
    mappings("net.fabricmc:yarn:$yarn_mappings:v2")
    val loader_version: String by project
    modImplementation("net.fabricmc:fabric-loader:$loader_version")
    val fabric_version: String by project
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabric_version")
    val fabricKotlinVersion: String by project
    modImplementation("net.fabricmc:fabric-language-kotlin:$fabricKotlinVersion")

    //kord
    implementation("dev.kord:kord-core:0.8.0")
    shadow("dev.kord:kord-core:0.8.0")
    implementation("dev.kord.x:emoji:0.5.0")
    shadow("dev.kord.x:emoji:0.5.0")

    implementation("io.netty:netty-all:4.1.79.Final")
}

tasks.remapJar {
    dependsOn(tasks.shadowJar)
    inputFile.set(tasks.shadowJar.get().archiveFile)
}

tasks.shadowJar {
    configurations = listOf(project.configurations.shadow.get())
}

tasks {
    val javaVersion = JavaVersion.VERSION_17
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
        options.release.set(javaVersion.toString().toInt())
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions { jvmTarget = javaVersion.toString() }
        //sourceCompatibility = javaVersion.toString()
        //targetCompatibility = javaVersion.toString()
    }
    jar { from("LICENSE") { rename { "${it}_${base.archivesName}" } } }
    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") { expand(mutableMapOf("version" to project.version)) }
    }
    java {
        toolchain { languageVersion.set(JavaLanguageVersion.of(javaVersion.toString())) }
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        withSourcesJar()
    }
}