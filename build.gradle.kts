import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	id("net.fabricmc.fabric-loom-remap")
	`maven-publish`
	id("org.jetbrains.kotlin.jvm")
}

version = "${property("mod_version")}+${property("minecraft_version_group")}"
group = property("maven_group") as String

base {
	archivesName = property("archives_base_name") as String
}

repositories {
	mavenCentral()
	maven("https://jitpack.io")
	maven("https://maven.terraformersmc.com/")
	maven("https://maven.shedaniel.me/")
	maven("https://maven.nucleoid.xyz/")
	maven("https://oss.sonatype.org/content/repositories/snapshots")
	maven("https://maven.siphalor.de/")
}

loom {
	splitEnvironmentSourceSets()

	mods {
		register("homabric") {
			sourceSet(sourceSets.main.get())
			sourceSet(sourceSets.getByName("client"))
		}
	}
}

val transitiveImplementation: Configuration = configurations.create("transitiveImplementation") {
	isTransitive = true
	exclude(group = "org.jetbrains.kotlin") // exclude anything kotlin related because there is already fabric kotlin
}

dependencies {
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
	mappings(loom.officialMojangMappings())
	modImplementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")

	// fabric api without bundling
	modCompileOnly("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
	modLocalRuntime("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")

	modImplementation(include("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")!!)
	modImplementation(include("me.lucko:fabric-permissions-api:${providers.gradleProperty("fabric_permissions_api_version").get()}")!!)
	modImplementation(include("eu.pb4:sgui:${providers.gradleProperty("sgui_version").get()}")!!)
	modImplementation(include("xyz.nucleoid:server-translations-api:${providers.gradleProperty("server_translations_api_version").get()}")!!)

	transitiveImplementation("org.spongepowered:configurate-hocon:${providers.gradleProperty("configurate_version").get()}")
	transitiveImplementation("org.spongepowered:configurate-extra-kotlin:${providers.gradleProperty("configurate_version").get()}")
}

transitiveImplementation.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
	val id = artifact.moduleVersion.id
	val notation = "${id.group}:${id.name}:${id.version}"
	dependencies.add("include", notation)
	dependencies.add("implementation", notation)
}

tasks.withType<JavaCompile>().configureEach {
	options.encoding = "UTF-8"
	options.release = 21
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_21
	}
}

java {
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

tasks.processResources {
	inputs.property("version", version)

	filesMatching("fabric.mod.json") {
		expand("version" to version)
	}
}

tasks.jar {
	val projectName = project.name
	inputs.property("projectName", projectName)

	from("LICENSE") {
		rename { "${it}_$projectName" }
	}
}

publishing {
	publications {
		register<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}

	repositories {}
}
