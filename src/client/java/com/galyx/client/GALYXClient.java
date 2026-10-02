package com.galyx.client;

import com.galyx.GALYX;
import com.galyx.client.config.ConfigManager;
import com.galyx.client.config.GalyxConfig;
import com.galyx.client.detectors.ActionBarBossDetector;
import com.galyx.client.detectors.BossDetector;
import com.galyx.client.detectors.CustomBossDetector;
import com.galyx.client.detectors.DefeatGuard;
import com.galyx.client.gui.ConfigScreen;
import com.galyx.client.gui.WelcomeWizardScreen;
import com.galyx.client.hud.HudManager;
import com.galyx.client.notify.BossSpawnNotifier;
import com.galyx.client.stats.SessionTracker;
import com.galyx.client.stats.StatisticsManager;
import com.galyx.client.trackers.BossTracker;
import com.galyx.client.trackers.CustomBossTracker;
import com.galyx.core.ModuleManager;
import com.galyx.events.EventBus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.ResourceLocation;

/**
 * Entrypoint del lado del cliente. Arma el ciclo completo de GALYX:
 *
 * BossDetector / CustomBossDetector / ActionBarBossDetector --(EventBus)-->
 *         BossTracker / CustomBossTracker --> StatisticsManager --> HudManager
 *
 * Cada pieza se registra en el ModuleManager para que su ciclo de vida
 * (initialize / enable / tick) se maneje de forma centralizada.
 */
public class GALYXClient implements ClientModInitializer {

    private final EventBus eventBus = new EventBus();
    private final ModuleManager moduleManager = new ModuleManager();
    private boolean openConfigScreenNextTick = false;
    private boolean openWizardNextTick = false;

    // Instancia compartida: tanto el comando /galyx como el HUD en vivo y
    // la integracion de ModMenu tienen que leer y escribir sobre el MISMO
    // objeto GalyxConfig. Si cada entrada crea su propio ConfigManager
    // (como hacia GalyxModMenuIntegration antes), guardar desde una no se
    // refleja en la otra hasta reiniciar el juego.
    private static GalyxConfig sharedConfig;
    private static ConfigManager sharedConfigManager;

    public static GalyxConfig getSharedConfig() {
        if (sharedConfig == null) {
            sharedConfigManager = new ConfigManager();
            sharedConfig = sharedConfigManager.load();
        }
        return sharedConfig;
    }

    public static ConfigManager getSharedConfigManager() {
        getSharedConfig();
        return sharedConfigManager;
    }

    @Override
    public void onInitializeClient() {
        ConfigManager configManager = new ConfigManager();
        GalyxConfig config = configManager.load();
        sharedConfig = config;
        sharedConfigManager = configManager;

        // Jefes vanilla (Ender Dragon, Wither)
        BossTracker bossTracker = new BossTracker(eventBus);
        BossDetector bossDetector = new BossDetector(eventBus);

        // Jefes de OlympoMC vistos por barra de vida vanilla (confirmados por item)
        CustomBossTracker customBossTracker = new CustomBossTracker(eventBus);
        CustomBossDetector customBossDetector = new CustomBossDetector(eventBus, config.customBosses);

        // Jefes de OlympoMC vistos por action bar (confirmados cuando su vida llega a 0,
        // o mas confiablemente, cuando aparece el loot de confirmacion tras vida baja).
        // Aplica a los 33, ya que no todos muestran la barra global de forma confiable.
        ActionBarBossDetector actionBarBossDetector = new ActionBarBossDetector(
                eventBus,
                config.customBosses
        );

        // Avisos de horario de spawn (accion-bar, 1 minuto antes)
        BossSpawnNotifier spawnNotifier = new BossSpawnNotifier(config);

        // Mide cuanto tiempo llevas en la sesion actual, para calcular jefes/hora
        SessionTracker sessionTracker = new SessionTracker();

        moduleManager.register(sessionTracker);
        moduleManager.register(bossDetector);
        moduleManager.register(bossTracker);
        moduleManager.register(customBossDetector);
        moduleManager.register(customBossTracker);
        moduleManager.register(actionBarBossDetector);
        moduleManager.register(spawnNotifier);

        moduleManager.initialize();
        moduleManager.enableAll();

        if (!config.bossDetectorEnabled) {
            bossDetector.disable();
        }

        // Reinicia todos los contadores cada vez que entras a un mundo o
        // servidor nuevo (privado u OlympoMC), para que nada se arrastre
        // de una sesion a otra.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            bossTracker.reset();
            customBossTracker.reset();
            customBossDetector.reset();
            actionBarBossDetector.reset();
            spawnNotifier.reset();
            sessionTracker.reset();
            DefeatGuard.reset();

            // Asistente de primera vez: solo se marca para abrir (no se abre
            // aqui mismo) porque, igual que con /galyx, el juego todavia esta
            // terminando de montar la pantalla de "conectando..." en este
            // instante -- se abre un tick despues, ya con el mundo estable.
            if (!config.onboardingCompleted) {
                openWizardNextTick = true;
            }
        });

        // OJO: la ventana de chat se cierra sola justo despues de correr el
        // comando, asi que si abrimos ConfigScreen aqui mismo (incluso con
        // Minecraft.execute(), que corre INMEDIATO si ya estamos en el hilo
        // correcto), el cierre del chat pisa la pantalla que acabamos de abrir
        // y nunca se ve. Por eso solo marcamos una bandera, y la pantalla se
        // abre de verdad un tick despues (mas abajo, en END_CLIENT_TICK),
        // cuando el chat ya termino de cerrarse por su cuenta.
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("galyx")
                        .executes(context -> {
                            openConfigScreenNextTick = true;
                            return 1;
                        })
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            moduleManager.tick();
            if (openConfigScreenNextTick) {
                openConfigScreenNextTick = false;
                client.setScreen(new ConfigScreen(config, configManager));
            }
            if (openWizardNextTick) {
                openWizardNextTick = false;
                client.setScreen(new WelcomeWizardScreen(config, configManager));
            }
        });

        StatisticsManager statisticsManager = new StatisticsManager(bossTracker, customBossTracker, sessionTracker);
        HudManager hudManager = new HudManager(statisticsManager, config);

        HudElementRegistry.addLast(
                ResourceLocation.fromNamespaceAndPath(GALYX.MOD_ID, "boss_stats"),
                hudManager::render
        );
    }

}