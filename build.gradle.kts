import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	id("net.fabricmc.fabric-loom")
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

val makeTransitive: Configuration = configurations.create("makeTransitive") {
	isTransitive = true
	exclude(group = "org.jetbrains.kotlin") // exclude anything kotlin related because there is already fabric kotlin
}

dependencies {
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
	implementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")

	// fabric api without bundling
	compileOnly("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
	localRuntime("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")

	implementation(include("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")!!)
	implementation(include("me.lucko:fabric-permissions-api:${providers.gradleProperty("fabric_permissions_api_version").get()}")!!)
	implementation(include("eu.pb4:sgui:${providers.gradleProperty("sgui_version").get()}")!!)
	implementation(include("xyz.nucleoid:server-translations-api:${providers.gradleProperty("server_translations_api_version").get()}")!!)

	makeTransitive("org.spongepowered:configurate-hocon:${providers.gradleProperty("configurate_version").get()}")
	makeTransitive("org.spongepowered:configurate-extra-kotlin:${providers.gradleProperty("configurate_version").get()}")
}

makeTransitive.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
	val id = artifact.moduleVersion.id
	val notation = "${id.group}:${id.name}:${id.version}"
	dependencies.add("include", notation)
	dependencies.add("implementation", notation)
}

tasks.withType<JavaCompile>().configureEach {
	options.encoding = "UTF-8"
	options.release = 25
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_25
	}
}

java {
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
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
