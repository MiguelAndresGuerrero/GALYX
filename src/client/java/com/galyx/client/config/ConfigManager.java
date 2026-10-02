package com.galyx.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Carga y guarda la configuracion de GALYX en <config>/galyx.json.
 * Si el archivo no existe (o esta corrupto), se recrea con los valores
 * por defecto para que el mod nunca falle por un config invalido.
 */
public class ConfigManager {

    private static final String FILE_NAME = "galyx.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configFile;
    private GalyxConfig config;

    public ConfigManager() {
        this.configFile = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public GalyxConfig load() {
        boolean fileExisted = Files.exists(configFile);

        if (fileExisted) {
            try (BufferedReader reader = Files.newBufferedReader(configFile)) {
                config = GSON.fromJson(reader, GalyxConfig.class);
            } catch (IOException e) {
                config = null;
            }
        }

        if (config == null) {
            config = new GalyxConfig();
            if (!fileExisted) {
                // Instalacion realmente nueva (el archivo no existia en
                // absoluto) -- se le muestra el asistente de primera vez.
                // Si el archivo SI existia pero estaba corrupto, se asume
                // que ya era un jugador existente y no se le vuelve a mostrar.
                config.onboardingCompleted = false;
            }
            save();
        }

        return config;
    }

    public void save() {
        try {
            // IMPORTANTE: bossSpawnTimes no tiene ningun boton en el menu que
            // lo edite -- solo se puede llenar a mano en el .json. Por eso,
            // antes de guardar, se vuelve a leer del ARCHIVO EN DISCO (no de
            // memoria) y se preserva tal cual este ahi. Sin esto, cualquier
            // "Guardar" del menu (aunque sea solo para mover el HUD) pisaba
            // el archivo completo con lo que hubiera en memoria y borraba
            // en silencio cualquier horario que se hubiera puesto a mano.
            if (Files.exists(configFile)) {
                try (BufferedReader reader = Files.newBufferedReader(configFile)) {
                    GalyxConfig onDisk = GSON.fromJson(reader, GalyxConfig.class);
                    if (onDisk != null && onDisk.bossSpawnTimes != null && !onDisk.bossSpawnTimes.isEmpty()) {
                        config.bossSpawnTimes = onDisk.bossSpawnTimes;
                    }
                } catch (Exception ignored) {
                    // Si el archivo en disco esta corrupto justo en este instante,
                    // se guarda con lo que haya en memoria en vez de fallar.
                }
            }

            Files.createDirectories(configFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(configFile)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            // Si falla el guardado, GALYX sigue funcionando con los valores en memoria.
        }
    }

    public GalyxConfig getConfig() {
        return config;
    }

}