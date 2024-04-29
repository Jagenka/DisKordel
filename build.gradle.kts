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
    maven("https://libraries.minecraft.net")
    maven("https://redempt.dev")
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
    implementation("dev.kord:kord-core:0.13.1")
    shadow("dev.kord:kord-core:0.13.1")
    implementation("dev.kord.x:emoji:0.5.0")
    shadow("dev.kord.x:emoji:0.5.0")

    //String comparison
    implementation("info.debatty:java-string-similarity:2.0.0")
    shadow("info.debatty:java-string-similarity:2.0.0")

    //eval command
    implementation("com.github.Redempt:Crunch:2.0.3")
    shadow("com.github.Redempt:Crunch:2.0.3")

    //brigardier
    //implementation("com.mojang:brigadier:1.0.18")
    //shadow("com.mojang:brigadier:1.0.18")

    implementation("io.netty:netty-all:4.1.79.Final")
}

loom {
    accessWidenerPath.set(file("src/main/resources/diskordel.accesswidener"))
}

tasks.remapJar {
    dependsOn(tasks.shadowJar)
    inputFile.set(tasks.shadowJar.get().archiveFile)
}

tasks.shadowJar {
    configurations = listOf(project.configurations.shadow.get())
}

tasks {
    withType<JavaCompile> {
        options.release.set(21)
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "21"
        }
    }
    jar { from("LICENSE") { rename { "${it}_${base.archivesName}" } } }
    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") { expand(mutableMapOf("version" to project.version)) }
    }

    java {
        // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
        // if it is present.
        // If you remove this line, sources will not be generated.
        withSourcesJar()

        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}