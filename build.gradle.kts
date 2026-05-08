plugins {
    id("net.fabricmc.fabric-loom") version "1.15.5"
    `maven-publish`
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

loom {
    // no mappings needed for unobfuscated MC 26.1
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    @Suppress("UnstableApiUsage")
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    @Suppress("UnstableApiUsage")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_api_version")}")
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to inputs.properties["version"]))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
