plugins {
	// La versión de Loom no cambia entre 1.21.8 y 1.21.11, así que va fija aquí
	// (dentro de "plugins {}" no se puede leer stonecutter.properties.toml dinámicamente).
	id("net.fabricmc.fabric-loom-remap") version "1.17-SNAPSHOT"
	id("maven-publish")
}

// Todas las propiedades se leen UNA vez aquí arriba, a nivel de proyecto.
// Dentro de los bloques de tareas (tasks.jar { ... }, etc.) "property(...)" deja
// de referirse al proyecto y apunta a la tarea, por eso no se usa ahí directamente.
val mcVersion = stonecutter.current.version
val modVersion = property("mod.version") as String
val modGroup = property("mod.group") as String
val modId = property("mod.id") as String
val mcCompat = property("mod.mc_compat") as String
val loaderVersion = property("deps.loader") as String
val fabricApiVersion = property("deps.fabric_api") as String
val modmenuVersion = property("deps.modmenu") as String

// version = "0.9.4-beta+1.21.8" / "0.9.4-beta+1.21.11" -> distingue los jars generados
version = "$modVersion+$mcVersion"
group = modGroup

repositories {
	maven("https://maven.terraformersmc.com/") { name = "Terraformers" }
}

loom {
	splitEnvironmentSourceSets()

	runs {
		named("client") {
			programArgs("--username", "Player165")
		}
	}

	mods {
		create("galyx") {
			sourceSet(sourceSets["main"])
			sourceSet(sourceSets["client"])
		}
	}
}

dependencies {
	// A cada versión le llega automáticamente su propia Minecraft + mappings + Fabric API,
	// definidos en stonecutter.properties.toml
	minecraft("com.mojang:minecraft:$mcVersion")
	mappings(loom.officialMojangMappings())
	modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
	modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

	// Integración opcional con Mod Menu (solo si el jugador ya lo tiene instalado).
	modCompileOnly("com.terraformersmc:modmenu:$modmenuVersion")
}

tasks.processResources {
	val props = mapOf(
		"version" to version.toString(),
		"minecraft" to mcCompat
	)
	inputs.properties(props)
	filesMatching("fabric.mod.json") {
		expand(props)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release.set(21)
}

java {
	withSourcesJar()
	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
	inputs.property("projectName", modId)
	from(rootProject.file("LICENSE")) {
		rename { "${it}_$modId" }
	}
}

// Copia el jar final (y su sources jar) a build/libs/<mod.version>/<minecraft>/
// para tener todos los builds de todas las versiones juntos y ordenados.
tasks.register<Copy>("buildAndCollect") {
	group = "build"
	from(tasks.named("remapJar"), tasks.named("remapSourcesJar"))
	into(rootProject.layout.buildDirectory.dir("libs/$modVersion/$mcVersion"))
	dependsOn("build")
}

publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}
	repositories {
		// Agrega aquí tus repos de publicación si los necesitas.
	}
}
