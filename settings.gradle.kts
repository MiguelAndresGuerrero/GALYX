pluginManagement {
	repositories {
		maven("https://maven.fabricmc.net/") { name = "Fabric" }
		maven("https://maven.terraformersmc.com/") { name = "Terraformers" }
		maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
		mavenCentral()
		gradlePluginPortal()
	}
}

plugins {
	id("dev.kikugie.stonecutter") version "0.9.4"
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
	create(rootProject) {
		// Esto genera automáticamente los subproyectos versions/1.21.8 y versions/1.21.11,
		// ambos compilando el MISMO código fuente compartido en /src.
		versions("1.21.8", "1.21.11")

		// Versión que se usa como referencia para el control de versiones (git).
		vcsVersion = "1.21.11"
	}
}

rootProject.name = "galyx"
