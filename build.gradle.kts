import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	id("net.fabricmc.fabric-loom")
	`maven-publish`
	id("org.jetbrains.kotlin.jvm")
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
	implementation(include("org.spongepowered:configurate-hocon:${providers.gradleProperty("configurate_version").get()}")!!)
	implementation(include("org.spongepowered:configurate-extra-kotlin:${providers.gradleProperty("configurate_version").get()}")!!)
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
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	// If you remove this line, sources will not be generated.
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
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

	repositories {
		// Add repositories to publish to here.
	}
}
