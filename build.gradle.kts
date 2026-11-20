import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("net.fabricmc.fabric-loom")
    kotlin("jvm")
}

val modVersion = property("mod_version") as String
val minecraftVersion = property("minecraft_version") as String
val loaderVersion = property("loader_version") as String
val fabricKotlinVersion = property("fabric_kotlin_version") as String

version = "$modVersion+$minecraftVersion"
group = property("maven_group") as String

base {
    archivesName.set(property("archives_base_name") as String)
}

repositories {
    maven("https://maven.terraformersmc.com/releases/") { name = "TerraformersMC" }
    maven("https://maven.isxander.dev/releases/") { name = "isXander" }
    maven("https://repo.polyfrost.org/releases/") { name = "Polyfrost" }
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
    mavenCentral()
}

loom {
    mods {
        register("mediaplayer") {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    implementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")

    implementation("dev.isxander:yet-another-config-lib:${property("yacl_version")}")
    implementation("com.terraformersmc:modmenu:${property("modmenu_version")}")

    val oneConfigVersion = property("oneconfig_version") as String
    listOf(property("oneconfig_platform") as String, "config", "config-impl", "utils", "poly-compose").forEach { module ->
        compileOnly("org.polyfrost.oneconfig:$module:$oneConfigVersion") {
            isTransitive = false
        }
    }
}

tasks.processResources {
    val props = mapOf(
        "version" to version.toString(),
        "minecraft_version" to minecraftVersion,
        "loader_version" to loaderVersion,
        "fabric_kotlin_version" to fabricKotlinVersion,
    )
    inputs.properties(props)
    filesMatching("fabric.mod.json") {
        expand(props)
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}
