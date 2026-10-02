plugins {
	id("dev.kikugie.stonecutter")
}

// Versión que se abre por defecto en el IDE / al correr tareas sin especificar.
// Cámbiala con el task "Set active project to 1.21.x" o editando esta línea.
stonecutter active "1.21.8" /* [SC] DO NOT EDIT */

// Parámetros que Stonecutter sustituye automáticamente en el código fuente
// cuando compila cada versión. Ver https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
	swaps["mod_version"] = "\"${property("mod.version")}\";"
	swaps["minecraft"] = "\"${node.metadata.version}\";"
	dependencies["fapi"] = node.project.property("deps.fabric_api") as String

	// Mojang renombró la clase ResourceLocation -> Identifier a partir de la 1.21.11.
	// El código fuente está escrito con "ResourceLocation" (válido en 1.21.8);
	// Stonecutter lo convierte automáticamente a "Identifier" al compilar 1.21.11+.
	replacements {
		string(current.parsed >= "1.21.11") {
			replace("ResourceLocation", "Identifier")
		}
	}
}
