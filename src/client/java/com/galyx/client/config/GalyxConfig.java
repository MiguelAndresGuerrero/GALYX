package com.galyx.client.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GalyxConfig {

    // ---------------------------------------------------------------
    // General
    // ---------------------------------------------------------------
    public boolean hudEnabled = true;
    public boolean bossDetectorEnabled = true;
    public String language = "es";
    public boolean hour24Format = true;

    // Controla si ya se mostro el asistente de primera vez (WelcomeWizardScreen).
    // El default es "true" a proposito: si alguien ya tenia un galyx.json de
    // antes de que existiera este campo, Gson deja este valor en lo que puso
    // el constructor (true) porque la clave no esta en su JSON -- asi el
    // asistente NUNCA le aparece a alguien que ya estaba usando el mod.
    // Solo ConfigManager.load() lo fuerza a "false" cuando el archivo no
    // existia todavia (instalacion realmente nueva).
    public boolean onboardingCompleted = true;

    // ---------------------------------------------------------------
    // Posicion / escala del HUD
    // ---------------------------------------------------------------
    public int hudX = 8;
    public int hudY = 8;
    public float hudScale = 1.0f;

    // ---------------------------------------------------------------
    // Estilo del HUD
    // ---------------------------------------------------------------
    public boolean hudUseIcons = true;
    public boolean hudShowTotal = true;
    public int hudIconSize = 12;
    public int hudPanelAlpha = 144;
    public int hudBorderColorIndex = 0;
    public int hudSecondaryColorIndex = 2;
    public boolean hudRoundedCorners = true;
    public boolean hudShowBorder = true;
    public int hudAnchor = 0; // 0=Sup.Izq 1=Sup.Der 2=Inf.Izq 3=Inf.Der (hudX/hudY se miden desde esa esquina)
    public int hudBackgroundColorIndex = -1; // -1 = fondo oscuro neutro por defecto
    public boolean hudTextShadow = true;

    // ---------------------------------------------------------------
    // Contenido del HUD (estadisticas adicionales que puede mostrar)
    // ---------------------------------------------------------------
    public boolean hudShowPlaytime = false;
    public boolean hudShowFragments = false;
    public boolean hudShowFps = false;
    public boolean hudShowPing = false;
    public boolean hudShowCoords = false;

    // Lista de jefes derrotados en el HUD
    public int hudSortMode = 0; // 0=Prioridad 1=Mas derrotas 2=Alfabetico
    public int hudMaxBossesShown = 40; // limite de filas mostradas (40 cubre los 33 propios + los vanilla)

    // ---------------------------------------------------------------
    // Rendimiento
    // ---------------------------------------------------------------
    // Cada cuantos ticks (aprox) se recalculan los textos "pesados" del HUD
    // (proximo spawn, tiempo jugado, coordenadas...). 1 = cada tick.
    public int hudUpdateIntervalTicks = 1;

    // ---------------------------------------------------------------
    // Notificaciones de spawn
    // ---------------------------------------------------------------
    public boolean spawnNotifierSoundEnabled = true;
    public boolean notifyUseChat = false; // false = action bar, true = chat
    public int notifyWarningMinutes = 1;
    public String spawnNotifierTimezone = "America/Lima";

    // Nombre del jefe (tal como aparece en la barra de vida) -> texto del
    // item que confirma su muerte (tal como aparece en el nombre del drop).
    public Map<String, String> customBosses = new LinkedHashMap<>();
    // Solo aplica a los jefes de /boss (evento programado). Los de survival no tienen horario fijo.
    public Map<String, List<String>> bossSpawnTimes = new LinkedHashMap<>();

    // ---------------------------------------------------------------
    // Configuracion por jefe. Si un jefe no aparece en estos mapas se
    // asume: habilitado = true, prioridad = 1 (Media), color = -1 (usa
    // el color secundario del HUD en vez de uno propio).
    // ---------------------------------------------------------------
    public Map<String, Boolean> bossEnabled = new LinkedHashMap<>();
    public Map<String, Integer> bossPriority = new LinkedHashMap<>();
    public Map<String, Integer> bossColorIndex = new LinkedHashMap<>();

    // Nombre exacto del item "cabeza" de cada jefe de mundo (los unicos 6 que
    // sueltan una propia, segun el comentario de arriba), tal como aparece
    // en su tooltip en el inventario. Se usa para contar cuantas tiene el
    // jugador -- es editable desde el menu por si alguno de estos nombres
    // "de memoria" no calza exacto con el item real del server.
    public Map<String, String> bossHeadNames = new LinkedHashMap<>();

    {
        // Confirmados con cabeza propia
        customBosses.put("REY NEPTUNO", "REY NEPTUNO");
        customBosses.put("REY PIGLIN", "REY PIGLIN");
        customBosses.put("AZAZEL", "AZAZEL");
        customBosses.put("REY GOBLIN", "REY GOBLIN");
        customBosses.put("PALADÍN ESQUELETOR", "PALADÍN ESQUELETOR");
        customBosses.put("CLEOPATRA", "CLEOPATRA");

        // Mejor intento a partir del tooltip real visto en juego
        // ("Cabeza del Rey Piglin"). Si alguno de estos 6 no calza exacto
        // con el item real del server, se corrige desde /galyx -> Informacion.
        bossHeadNames.put("REY NEPTUNO", "Cabeza del Rey Neptuno");
        bossHeadNames.put("REY PIGLIN", "Cabeza del Rey Piglin");
        bossHeadNames.put("AZAZEL", "Cabeza de Azazel");
        bossHeadNames.put("REY GOBLIN", "Cabeza del Rey Goblin");
        bossHeadNames.put("PALADÍN ESQUELETOR", "Cabeza del Paladín Esqueletor");
        bossHeadNames.put("CLEOPATRA", "Cabeza de Cleopatra");

        // Sueltan Cristal de Olympium
        customBosses.put("PERSEO", "CRISTAL DE OLYMPIUM");
        customBosses.put("RAGNAR", "CRISTAL DE OLYMPIUM");
        customBosses.put("AKAINU", "CRISTAL DE OLYMPIUM");
        customBosses.put("ÍCARO", "CRISTAL DE OLYMPIUM");
        customBosses.put("ARTEMISA", "CRISTAL DE OLYMPIUM");
        customBosses.put("AQUILES", "CRISTAL DE OLYMPIUM");
        customBosses.put("FARAÓN", "CRISTAL DE OLYMPIUM");
        customBosses.put("TRITÓN", "CRISTAL DE OLYMPIUM");
        customBosses.put("LEONIDAS", "CRISTAL DE OLYMPIUM");
        customBosses.put("MELINOE", "CRISTAL DE OLYMPIUM");
        customBosses.put("MEDUSA", "CRISTAL DE OLYMPIUM");
        customBosses.put("NARAKU", "CRISTAL DE OLYMPIUM");
        customBosses.put("SIGURD", "CRISTAL DE OLYMPIUM");
        customBosses.put("RORONOA", "CRISTAL DE OLYMPIUM");
        customBosses.put("ANTIOPE", "CRISTAL DE OLYMPIUM");
        customBosses.put("VALKIRIA", "CRISTAL DE OLYMPIUM");
        customBosses.put("ORFEO", "CRISTAL DE OLYMPIUM");
        customBosses.put("VLAD", "CRISTAL DE OLYMPIUM");
        customBosses.put("BERSERKER", "CRISTAL DE OLYMPIUM");
        customBosses.put("TYRANT", "CRISTAL DE OLYMPIUM");
        customBosses.put("OPTIMUS", "CRISTAL DE OLYMPIUM");

        // Sueltan Fragmento de Zafiro
        customBosses.put("IGNEL", "FRAGMENTO DE ZAFIRO");
        customBosses.put("NEKOYAMI", "FRAGMENTO DE ZAFIRO");
        customBosses.put("OBERON", "FRAGMENTO DE ZAFIRO");
        customBosses.put("RYUK", "FRAGMENTO DE ZAFIRO");
        customBosses.put("MUZAN", "FRAGMENTO DE ZAFIRO");
        customBosses.put("TITANIA", "FRAGMENTO DE ZAFIRO");
    }

    /**
     * true si el jefe esta habilitado para mostrarse en el HUD.
     * Por defecto (jefe sin entrada en el mapa) se considera habilitado.
     */
    public boolean isBossEnabled(String bossName) {
        Boolean value = bossEnabled.get(bossName);
        return value == null || value;
    }

    /**
     * Prioridad del jefe: 0 = Alta, 1 = Media (por defecto), 2 = Baja.
     * Se usa para ordenar la lista de jefes derrotados en el HUD.
     */
    public int getBossPriority(String bossName) {
        Integer value = bossPriority.get(bossName);
        return value == null ? 1 : value;
    }

    /**
     * Indice de color propio del jefe dentro de HudTheme.BORDER_COLORS,
     * o -1 si debe usar el color secundario general del HUD.
     */
    public int getBossColorIndex(String bossName) {
        Integer value = bossColorIndex.get(bossName);
        return value == null ? -1 : value;
    }

}