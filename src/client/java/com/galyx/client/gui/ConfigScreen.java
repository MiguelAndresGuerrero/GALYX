package com.galyx.client.gui;

import com.galyx.GALYX;
import com.galyx.client.config.ConfigManager;
import com.galyx.client.config.GalyxConfig;
import com.galyx.client.gui.widget.GalyxSlider;
import com.galyx.client.gui.widget.SwatchButton;
import com.galyx.client.gui.widget.TabButton;
import com.galyx.client.gui.widget.ToggleSwitch;
import com.galyx.client.hud.HudTheme;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
//? if >=1.21.9 {
/*import net.minecraft.client.input.MouseButtonEvent;*/
//?}
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pantalla de configuracion de GALYX. Organizada en pestañas (General,
 * Apariencia, HUD, Jefes, Notificaciones, Avanzado, Informacion) con una
 * vista previa en vivo del HUD que refleja los cambios sin guardarlos.
 *
 * Todo lo que el usuario toca queda en campos "pending*" hasta que pulsa
 * "Guardar cambios" -- "Cancelar" descarta todo sin tocar el config real,
 * igual que la pantalla original.
 */
public class ConfigScreen extends Screen {

    public enum Tab {
        GENERAL("General", "\u25c7", true),
        APARIENCIA("Apariencia", "\u25c8", true),
        HUD("Contenido HUD", "\u25a6", true),
        JEFES("Jefes", "\u2694", false),
        NOTIFICACIONES("Notificaciones", "\u266a", false),
        AVANZADO("Avanzado", "\u2699", false),
        INFORMACION("Informacion", "\u2139", false);

        final String label;
        final String icon;
        final boolean showsPreview;

        Tab(String label, String icon, boolean showsPreview) {
            this.label = label;
            this.icon = icon;
            this.showsPreview = showsPreview;
        }
    }

    private static final ResourceLocation LOGO = ResourceLocation.fromNamespaceAndPath(GALYX.MOD_ID, "icon.png");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final int COLOR_SUBTITLE = 0xFFAAAAAA;
    private static final int COLOR_LABEL = 0xFFE8E8E8;
    private static final int COLOR_HINT = 0xFF888890;
    private static final int COLOR_STATUS_OK = 0xFF7CE87C;
    private static final int COLOR_STATUS_ERR = 0xFFFF6666;
    private static final int COLOR_PANEL_BG_TOP = 0xE0181022;
    private static final int COLOR_PANEL_BG_BOTTOM = 0xE00A0A10;
    private static final int COLOR_SIDEBAR_BG = 0x40000000;
    private static final int COLOR_PREVIEW_BG = 0xFF0B0B12;

    private static final int PANEL_MARGIN = 8;
    private static final int SIDEBAR_WIDTH = 86;
    private static final int GAP = 6;
    private static final int IDEAL_CONTENT_WIDTH = 224;
    private static final int IDEAL_PREVIEW_WIDTH = 140;
    private static final int IDEAL_PANEL_WIDTH_WITH_PREVIEW = SIDEBAR_WIDTH + GAP + IDEAL_CONTENT_WIDTH + GAP + IDEAL_PREVIEW_WIDTH;
    private static final int MIN_CONTENT_WIDTH = 170;
    private static final int CORNER_SIZE = 6;
    private static final int HEADER_HEIGHT = 32;
    private static final int IDEAL_BODY_HEIGHT = 216;
    private static final int MIN_BODY_HEIGHT = 160;
    private static final int FOOTER_HEIGHT = 26;

    private static final int[] UPDATE_INTERVALS = {1, 5, 10, 20};

    // Zonas horarias mas comunes entre los jugadores de OlympoMC (server
    // hispanohablante). Se muestran como botones para que la mayoria no
    // tenga que escribir nada -- el campo de texto sigue abajo para
    // cualquier otra zona que no este en esta lista corta.
    private static final String[] TIMEZONE_PRESET_IDS = {
            "America/Bogota", "America/Lima", "America/Mexico_City", "America/Santiago",
            "America/Argentina/Buenos_Aires", "America/Caracas", "Europe/Madrid"
    };
    private static final String[] TIMEZONE_PRESET_LABELS = {
            "Colombia", "Peru", "Mexico", "Chile", "Argentina", "Venezuela", "Espana"
    };

    private final GalyxConfig config;
    private final ConfigManager configManager;
    private final Screen parentScreen;

    private Tab selectedTab;
    private String statusMessage = "";

    // --- Layout calculado en init() (se recalcula segun this.width/this.height
    // en cada apertura, para que se adapte a la escala de GUI del usuario) ---
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int bodyHeight;
    private int sidebarX;
    private int contentX;
    private int contentWidth;
    private int previewX;
    private int previewY;
    private int previewWidth;
    private int previewHeight;
    private boolean previewVisible;

    private final List<Label> labels = new ArrayList<>();

    // --- Estado pendiente (no se aplica a config hasta Guardar) ---
    private boolean pendingHudEnabled;
    private boolean pendingBossDetector;
    private String pendingTimezone;

    private int pendingHudX;
    private int pendingHudY;
    private float pendingHudScale;

    private boolean pendingUseIcons;
    private boolean pendingShowTotal;
    private int pendingIconSize;
    private int pendingPanelAlpha;
    private int pendingPrimaryColor;
    private int pendingSecondaryColor;
    private boolean pendingRounded;
    private boolean pendingShowBorder;
    private int pendingAnchor;
    private int pendingBackgroundColor;
    private boolean pendingTextShadow;

    private boolean pendingShowPlaytime;
    private boolean pendingShowFragments;
    private boolean pendingShowFps;
    private boolean pendingShowPing;
    private boolean pendingShowCoords;

    private int pendingSortMode;
    private int pendingMaxBossesShown;

    private int pendingUpdateIntervalTicks;

    private boolean pendingSoundEnabled;
    private boolean pendingUseChat;
    private int pendingWarningMinutes;

    private final Map<String, Boolean> pendingBossEnabled = new LinkedHashMap<>();
    private final Map<String, Integer> pendingBossPriority = new LinkedHashMap<>();
    private final Map<String, Integer> pendingBossColor = new LinkedHashMap<>();
    private final Map<String, String> pendingBossHeadNames = new LinkedHashMap<>();
    private boolean editingHeadNames = false;
    private int jefesPage = 0;
    private static final int JEFES_ROWS_PER_PAGE = 4;

    // "TODOS" o el nombre exacto del grupo (coincide con el valor que
    // customBosses.get(nombre) tiene para los jefes de /boss: "CRISTAL DE
    // OLYMPIUM" o "FRAGMENTO DE ZAFIRO"; los jefes de mundo se detectan
    // porque su "drop" es su propio nombre -- ver bossGroupOf()).
    private static final String GROUP_ALL = "TODOS";
    private static final String GROUP_WORLD = "MUNDO";
    private String jefesGroupFilter = GROUP_ALL;

    // --- Arrastrar la vista previa para reposicionar el HUD ---
    private boolean dragArmed = false;
    private boolean dragging = false;

    public ConfigScreen(GalyxConfig config, ConfigManager configManager) {
        this(config, configManager, null, Tab.GENERAL);
    }

    public ConfigScreen(GalyxConfig config, ConfigManager configManager, Screen parentScreen) {
        this(config, configManager, parentScreen, Tab.GENERAL);
    }

    public ConfigScreen(GalyxConfig config, ConfigManager configManager, Screen parentScreen, Tab initialTab) {
        super(Component.literal("GALYX"));
        this.config = config;
        this.configManager = configManager;
        this.parentScreen = parentScreen;
        this.selectedTab = initialTab != null ? initialTab : Tab.GENERAL;
        loadPendingFromConfig();
    }

    private void loadPendingFromConfig() {
        pendingHudEnabled = config.hudEnabled;
        pendingBossDetector = config.bossDetectorEnabled;
        pendingTimezone = config.spawnNotifierTimezone;

        pendingHudX = config.hudX;
        pendingHudY = config.hudY;
        pendingHudScale = config.hudScale;

        pendingUseIcons = config.hudUseIcons;
        pendingShowTotal = config.hudShowTotal;
        pendingIconSize = config.hudIconSize;
        pendingPanelAlpha = config.hudPanelAlpha;
        pendingPrimaryColor = config.hudBorderColorIndex;
        pendingSecondaryColor = config.hudSecondaryColorIndex;
        pendingRounded = config.hudRoundedCorners;
        pendingShowBorder = config.hudShowBorder;
        pendingAnchor = config.hudAnchor;
        pendingBackgroundColor = config.hudBackgroundColorIndex;
        pendingTextShadow = config.hudTextShadow;

        pendingShowPlaytime = config.hudShowPlaytime;
        pendingShowFragments = config.hudShowFragments;
        pendingShowFps = config.hudShowFps;
        pendingShowPing = config.hudShowPing;
        pendingShowCoords = config.hudShowCoords;

        pendingSortMode = config.hudSortMode;
        pendingMaxBossesShown = config.hudMaxBossesShown;

        pendingUpdateIntervalTicks = config.hudUpdateIntervalTicks;

        pendingSoundEnabled = config.spawnNotifierSoundEnabled;
        pendingUseChat = config.notifyUseChat;
        pendingWarningMinutes = config.notifyWarningMinutes;

        pendingBossEnabled.clear();
        pendingBossPriority.clear();
        pendingBossColor.clear();
        for (String bossName : config.customBosses.keySet()) {
            pendingBossEnabled.put(bossName, config.isBossEnabled(bossName));
            pendingBossPriority.put(bossName, config.getBossPriority(bossName));
            pendingBossColor.put(bossName, config.getBossColorIndex(bossName));
        }

        pendingBossHeadNames.clear();
        pendingBossHeadNames.putAll(config.bossHeadNames);
    }

    // -----------------------------------------------------------------
    // Layout / init
    // -----------------------------------------------------------------

    @Override
    protected void init() {
        labels.clear();

        // --- Ancho: se ajusta al ancho real de la ventana (this.width),
        // que en Minecraft depende de la escala de GUI del usuario. Si no
        // entra el panel "ideal" con vista previa, se quita la vista previa
        // primero antes de reducir el resto.
        int availableWidth = Math.max(SIDEBAR_WIDTH + GAP + MIN_CONTENT_WIDTH, this.width - PANEL_MARGIN * 2 - 8);

        boolean wantsPreview = selectedTab.showsPreview;
        if (wantsPreview && availableWidth >= IDEAL_PANEL_WIDTH_WITH_PREVIEW) {
            previewVisible = true;
            panelWidth = IDEAL_PANEL_WIDTH_WITH_PREVIEW;
            contentWidth = IDEAL_CONTENT_WIDTH;
            previewWidth = IDEAL_PREVIEW_WIDTH;
        } else {
            previewVisible = false;
            panelWidth = Math.min(IDEAL_PANEL_WIDTH_WITH_PREVIEW, availableWidth);
            contentWidth = Math.max(MIN_CONTENT_WIDTH, panelWidth - SIDEBAR_WIDTH - GAP);
            previewWidth = IDEAL_PREVIEW_WIDTH;
        }

        // --- Alto: idem, ajustado a this.height. Las pestañas se
        // construyeron para caber en IDEAL_BODY_HEIGHT; si la ventana es
        // mas baja que eso se reduce hasta MIN_BODY_HEIGHT (las filas mas
        // apretadas siguen siendo usables aunque no quede tan espaciado).
        // Apariencia ahora tiene mas contenido (paleta agrupada + panel de
        // "Contenido HUD" rapido), asi que pide un poco mas de alto que el
        // resto de pestañas si la ventana lo permite.
        int idealBodyHeightForTab = IDEAL_BODY_HEIGHT;
        if (selectedTab == Tab.APARIENCIA) {
            idealBodyHeightForTab += 46;
        } else if (selectedTab == Tab.INFORMACION) {
            idealBodyHeightForTab += editingHeadNames ? 160 : 20;
        }
        int availableHeight = Math.max(HEADER_HEIGHT + MIN_BODY_HEIGHT + FOOTER_HEIGHT, this.height - PANEL_MARGIN * 2 - 16);
        bodyHeight = Math.max(MIN_BODY_HEIGHT, Math.min(idealBodyHeightForTab, availableHeight - HEADER_HEIGHT - FOOTER_HEIGHT));
        panelHeight = HEADER_HEIGHT + bodyHeight + FOOTER_HEIGHT;

        int centerX = this.width / 2;
        panelLeft = centerX - panelWidth / 2;
        int idealTop = this.height / 2 - panelHeight / 2;
        int maxTop = Math.max(PANEL_MARGIN, this.height - panelHeight - PANEL_MARGIN);
        panelTop = Math.min(Math.max(PANEL_MARGIN, idealTop), maxTop);

        sidebarX = panelLeft;
        contentX = sidebarX + SIDEBAR_WIDTH + GAP;

        previewHeight = bodyHeight;
        previewX = contentX + (previewVisible ? contentWidth + GAP : 0);
        previewY = panelTop + HEADER_HEIGHT;

        buildSidebar();
        buildFooter();

        switch (selectedTab) {
            case GENERAL -> buildGeneralTab();
            case APARIENCIA -> buildAparienciaTab();
            case HUD -> buildHudContentTab();
            case JEFES -> buildJefesTab();
            case NOTIFICACIONES -> buildNotificacionesTab();
            case AVANZADO -> buildAvanzadoTab();
            case INFORMACION -> buildInformacionTab();
        }
    }

    private void switchTab(Tab tab) {
        // Si es la misma pestaña (usado para refrescar tras cambiar un
        // color, prioridad, forma, o pasar de pagina en Jefes) NO se
        // reinicia el estado de esa pestaña -- solo se reconstruye la
        // vista. El reinicio de jefesPage/dragArmed solo aplica cuando
        // el usuario realmente se mueve a OTRA pestaña.
        boolean changingTab = tab != this.selectedTab;
        this.selectedTab = tab;
        if (changingTab) {
            this.jefesPage = 0;
            this.jefesGroupFilter = GROUP_ALL;
            this.editingHeadNames = false;
            this.dragArmed = false;
            this.dragging = false;
        }
        this.clearWidgets();
        this.init();
    }

    private void buildSidebar() {
        int y = panelTop + HEADER_HEIGHT;
        for (Tab tab : Tab.values()) {
            TabButton button = new TabButton(this.font, sidebarX, y, SIDEBAR_WIDTH, 18, tab.icon + " " + tab.label,
                    tab == selectedTab, clicked -> switchTab(tab));
            button.setAccentColor(HudTheme.colorAt(pendingPrimaryColor));
            addRenderableWidget(button);
            y += 19;
        }
    }

    private void buildFooter() {
        int buttonsY = panelTop + panelHeight - FOOTER_HEIGHT + 3;
        int halfWidth = (panelWidth - 6) / 2;

        // Antes eran 3 botones siempre visibles (Guardar/Restablecer/Cancelar).
        // Ahora solo quedan las dos acciones de la barra de "cambios sin
        // guardar": Descartar (cierra sin tocar el config real -- el pending*
        // nunca se escribe a disco hasta Guardar) y Guardar cambios. El texto
        // de arriba (franja de estado) cambia solo, en render(), segun
        // isDirty() -- no hace falta reconstruir estos botones para eso.
        // "Restablecer a valores por defecto" se movio a la pestaña Avanzado.
        addRenderableWidget(Button.builder(Component.literal("Descartar"), b -> onClose())
                .bounds(panelLeft, buttonsY, halfWidth, 18)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Guardar cambios"), b -> save())
                .bounds(panelLeft + halfWidth + 6, buttonsY, halfWidth, 18)
                .build());
    }

    /**
     * true si algun valor "pending" (lo que se ve/edita en pantalla) difiere
     * de lo que hay realmente guardado en config ahora mismo. Se recalcula
     * en cada frame (es barato) en vez de ir marcando un flag en cada
     * callback -- asi no hay que tocar los ~40 sitios que modifican un
     * pending* cada vez que se agrega un campo nuevo.
     */
    private boolean isDirty() {
        return pendingHudEnabled != config.hudEnabled
                || pendingBossDetector != config.bossDetectorEnabled
                || !java.util.Objects.equals(pendingTimezone, config.spawnNotifierTimezone)
                || pendingHudX != config.hudX
                || pendingHudY != config.hudY
                || Float.compare(pendingHudScale, config.hudScale) != 0
                || pendingUseIcons != config.hudUseIcons
                || pendingShowTotal != config.hudShowTotal
                || pendingIconSize != config.hudIconSize
                || pendingPanelAlpha != config.hudPanelAlpha
                || pendingPrimaryColor != config.hudBorderColorIndex
                || pendingSecondaryColor != config.hudSecondaryColorIndex
                || pendingRounded != config.hudRoundedCorners
                || pendingShowBorder != config.hudShowBorder
                || pendingAnchor != config.hudAnchor
                || pendingBackgroundColor != config.hudBackgroundColorIndex
                || pendingTextShadow != config.hudTextShadow
                || pendingShowPlaytime != config.hudShowPlaytime
                || pendingShowFragments != config.hudShowFragments
                || pendingShowFps != config.hudShowFps
                || pendingShowPing != config.hudShowPing
                || pendingShowCoords != config.hudShowCoords
                || pendingSortMode != config.hudSortMode
                || pendingMaxBossesShown != config.hudMaxBossesShown
                || pendingUpdateIntervalTicks != config.hudUpdateIntervalTicks
                || pendingSoundEnabled != config.spawnNotifierSoundEnabled
                || pendingUseChat != config.notifyUseChat
                || pendingWarningMinutes != config.notifyWarningMinutes
                || !pendingBossEnabled.equals(config.bossEnabled)
                || !pendingBossPriority.equals(config.bossPriority)
                || !pendingBossColor.equals(config.bossColorIndex)
                || !pendingBossHeadNames.equals(config.bossHeadNames);
    }

    // -----------------------------------------------------------------
    // Pestaña: General
    // -----------------------------------------------------------------

    private void buildGeneralTab() {
        int y = panelTop + HEADER_HEIGHT;
        int width = currentContentWidth();

        addLabel("GENERAL", contentX, y, HudTheme.colorAt(pendingPrimaryColor));
        y += 11;

        addRenderableWidget(new ToggleSwitch(this.font, contentX, y, width, "Mostrar HUD", pendingHudEnabled,
                v -> pendingHudEnabled = v));
        y += 18;

        addRenderableWidget(new ToggleSwitch(this.font, contentX, y, width, "Detectar jefes", pendingBossDetector,
                v -> pendingBossDetector = v));
        y += 22;

        addLabel("ZONA HORARIA", contentX, y, HudTheme.colorAt(pendingPrimaryColor));
        y += 11;

        // Grid de 2 columnas con los paises mas comunes -- un clic basta,
        // sin tener que escribir el nombre de la zona a mano.
        int presetGap = 6;
        int presetWidth = (width - presetGap) / 2;
        int presetHeight = 14;
        for (int i = 0; i < TIMEZONE_PRESET_IDS.length; i++) {
            String zoneId = TIMEZONE_PRESET_IDS[i];
            boolean selected = zoneId.equals(pendingTimezone == null ? "" : pendingTimezone.trim());
            int col = i % 2;
            int row = i / 2;
            int px = contentX + col * (presetWidth + presetGap);
            int py = y + row * (presetHeight + 3);
            addRenderableWidget(Button.builder(
                            Component.literal((selected ? "\u25cf " : "\u25cb ") + TIMEZONE_PRESET_LABELS[i]),
                            b -> { pendingTimezone = zoneId; switchTab(Tab.GENERAL); })
                    .bounds(px, py, presetWidth, presetHeight)
                    .build());
        }
        int presetRows = (TIMEZONE_PRESET_IDS.length + 1) / 2;
        y += presetRows * (presetHeight + 3) + 6;

        addLabel("Otra zona (ej: America/Caracas):", contentX, y, COLOR_HINT);
        y += 9;
        EditBox timezoneBox = new EditBox(this.font, contentX, y, width, 16, Component.literal("Zona horaria"));
        timezoneBox.setMaxLength(48);
        timezoneBox.setValue(pendingTimezone);
        timezoneBox.setResponder(text -> pendingTimezone = text);
        addRenderableWidget(timezoneBox);
        y += 22;

        addLabel("Se usa para los avisos de spawn y la linea", contentX, y, COLOR_HINT);
        y += 9;
        addLabel("\"Proximo jefe\" del HUD.", contentX, y, COLOR_HINT);

        buildPreview();
    }

    // -----------------------------------------------------------------
    // Pestaña: Apariencia
    // -----------------------------------------------------------------

    private void buildAparienciaTab() {
        int colGap = 10;
        int colWidth = (currentContentWidth() - colGap) / 2;
        int leftX = contentX;
        int rightX = contentX + colWidth + colGap;

        int y = panelTop + HEADER_HEIGHT;

        // --- Columna izquierda: posicion y escala, + paleta debajo ---
        int ly = y;
        addLabel("POSICI\u00d3N Y ESCALA", leftX, ly, HudTheme.colorAt(pendingPrimaryColor));
        ly += 11;

        addStepperRow("X", leftX, ly, colWidth, 1, -50, 900, pendingHudX, v -> pendingHudX = v);
        ly += 16;
        addStepperRow("Y", leftX, ly, colWidth, 1, -50, 600, pendingHudY, v -> pendingHudY = v);
        ly += 16;
        addStepperRowFloat("Escala", leftX, ly, colWidth, 0.05f, 0.5f, 2.0f, pendingHudScale,
                v -> pendingHudScale = v);
        ly += 18;

        addRenderableWidget(Button.builder(
                        Component.literal("Anclaje: " + HudTheme.ANCHOR_NAMES[pendingAnchor]),
                        b -> { pendingAnchor = (pendingAnchor + 1) % HudTheme.ANCHOR_NAMES.length; switchTab(selectedTab); })
                .bounds(leftX, ly, colWidth, 15)
                .build());
        ly += 18;

        Button moveButton = Button.builder(
                        Component.literal(dragArmed ? "Arrastra la vista previa" : "[ Mover HUD ]"),
                        b -> {
                            dragArmed = !dragArmed;
                            b.setMessage(Component.literal(dragArmed ? "Arrastra la vista previa" : "[ Mover HUD ]"));
                        })
                .bounds(leftX, ly, colWidth, 15)
                .build();
        addRenderableWidget(moveButton);
        ly += 19;

        addRenderableWidget(Button.builder(
                        Component.literal((pendingRounded ? "\u25cf " : "\u25cb ") + "Redondeada"),
                        b -> { pendingRounded = true; switchTab(selectedTab); })
                .bounds(leftX, ly, colWidth, 14)
                .build());
        ly += 15;
        addRenderableWidget(Button.builder(
                        Component.literal((!pendingRounded ? "\u25cf " : "\u25cb ") + "Cuadrada"),
                        b -> { pendingRounded = false; switchTab(selectedTab); })
                .bounds(leftX, ly, colWidth, 14)
                .build());
        ly += 19;

        addLabel("PALETA", leftX, ly, HudTheme.colorAt(pendingPrimaryColor));
        ly += 11;

        addLabel("Primario \u00b7 " + HudTheme.nameAt(pendingPrimaryColor), leftX, ly, COLOR_HINT);
        ly += 9;
        buildColorRow(leftX, ly, colWidth, pendingPrimaryColor, index -> {
            pendingPrimaryColor = index;
            switchTab(selectedTab);
        });
        ly += 15;

        addLabel("Secundario \u00b7 " + HudTheme.nameAt(pendingSecondaryColor), leftX, ly, COLOR_HINT);
        ly += 9;
        buildColorRow(leftX, ly, colWidth, pendingSecondaryColor, index -> {
            pendingSecondaryColor = index;
            switchTab(selectedTab);
        });
        ly += 15;

        addLabel("Fondo del panel", leftX, ly, COLOR_HINT);
        ly += 9;
        SwatchButton bgSwatch = new SwatchButton(leftX, ly, 14,
                pendingBackgroundColor < 0 ? HudTheme.DEFAULT_BACKGROUND : HudTheme.colorAt(pendingBackgroundColor),
                "Fondo", false, s -> {
            int next = pendingBackgroundColor + 1;
            if (next >= HudTheme.BORDER_COLORS.length) {
                next = -1;
            }
            pendingBackgroundColor = next;
            switchTab(selectedTab);
        });
        if (pendingBackgroundColor < 0) {
            bgSwatch.setGlyph(this.font, "A");
        }
        addRenderableWidget(bgSwatch);

        // --- Columna derecha: estilo del HUD + contenido rapido ---
        int ry = y;
        addLabel("ESTILO DEL HUD", rightX, ry, HudTheme.colorAt(pendingPrimaryColor));
        ry += 11;

        addRenderableWidget(new ToggleSwitch(this.font, rightX, ry, colWidth, "Usar iconos",
                pendingUseIcons, v -> pendingUseIcons = v));
        ry += 14;

        addRenderableWidget(new ToggleSwitch(this.font, rightX, ry, colWidth, "Mostrar borde",
                pendingShowBorder, v -> pendingShowBorder = v));
        ry += 16;

        addStepperRow("Icono", rightX, ry, colWidth, 1, 4, 32, pendingIconSize, v -> pendingIconSize = v);
        ry += 16;
        addStepperRow("Opacidad", rightX, ry, colWidth, 5, 0, 255, pendingPanelAlpha, v -> pendingPanelAlpha = v);
        ry += 18;

        addRenderableWidget(new ToggleSwitch(this.font, rightX, ry, colWidth, "Sombra de texto",
                pendingTextShadow, v -> pendingTextShadow = v));
        ry += 20;

        // Panel de "Contenido HUD" rapido: los mismos campos que la pestaña
        // dedicada, como atajo para no tener que cambiar de pestaña para
        // prender/apagar una linea mientras se ajusta el estilo.
        addLabel("CONTENIDO HUD", rightX, ry, HudTheme.colorAt(pendingPrimaryColor));
        ry += 11;

        addRenderableWidget(new ToggleSwitch(this.font, rightX, ry, colWidth, "Proximo jefe",
                pendingShowTotal, v -> pendingShowTotal = v));
        ry += 13;
        addRenderableWidget(new ToggleSwitch(this.font, rightX, ry, colWidth, "Fragmentos de Zafiro",
                pendingShowFragments, v -> pendingShowFragments = v));
        ry += 13;
        addRenderableWidget(new ToggleSwitch(this.font, rightX, ry, colWidth, "FPS",
                pendingShowFps, v -> pendingShowFps = v));
        ry += 13;
        addRenderableWidget(new ToggleSwitch(this.font, rightX, ry, colWidth, "Ping",
                pendingShowPing, v -> pendingShowPing = v));

        buildPreview();
    }

    /** Fila "[-]  Label: valor  [+]" para un entero, con limites min/max. */
    private void addStepperRow(String label, int x, int y, int width, int step, int min, int max,
                               int current, java.util.function.IntConsumer setter) {
        int btnSize = 14;
        addRenderableWidget(Button.builder(Component.literal("-"), b -> {
                    setter.accept(Math.max(min, current - step));
                    switchTab(selectedTab);
                })
                .bounds(x, y, btnSize, btnSize)
                .build());

        addLabel(label + ": " + current, x + btnSize + 4, y + 3, COLOR_LABEL);

        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
                    setter.accept(Math.min(max, current + step));
                    switchTab(selectedTab);
                })
                .bounds(x + width - btnSize, y, btnSize, btnSize)
                .build());
    }

    /** Igual que addStepperRow pero para un valor decimal (ej. la escala del HUD). */
    private void addStepperRowFloat(String label, int x, int y, int width, float step, float min, float max,
                                    float current, java.util.function.Consumer<Float> setter) {
        int btnSize = 14;
        addRenderableWidget(Button.builder(Component.literal("-"), b -> {
                    setter.accept(Math.max(min, round2(current - step)));
                    switchTab(selectedTab);
                })
                .bounds(x, y, btnSize, btnSize)
                .build());

        addLabel(label + ": " + String.format("%.2f", current), x + btnSize + 4, y + 3, COLOR_LABEL);

        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
                    setter.accept(Math.min(max, round2(current + step)));
                    switchTab(selectedTab);
                })
                .bounds(x + width - btnSize, y, btnSize, btnSize)
                .build());
    }

    private float round2(float value) {
        return Math.round(value * 100f) / 100f;
    }

    private void buildColorRow(int x, int y, int width, int selectedIndex, java.util.function.IntConsumer onPick) {
        int count = HudTheme.BORDER_COLORS.length;
        int size = Math.max(8, Math.min(14, (width - (count - 1) * 2) / count));
        int cursor = x;
        for (int i = 0; i < count; i++) {
            int index = i;
            SwatchButton swatch = new SwatchButton(cursor, y, size, HudTheme.colorAt(i), HudTheme.nameAt(i),
                    i == selectedIndex, s -> onPick.accept(index));
            addRenderableWidget(swatch);
            cursor += size + 2;
        }
    }

    // -----------------------------------------------------------------
    // Pestaña: Contenido del HUD
    // -----------------------------------------------------------------

    private void buildHudContentTab() {
        int y = panelTop + HEADER_HEIGHT;
        int width = currentContentWidth();

        addLabel("CONTENIDO DEL HUD", contentX, y, HudTheme.colorAt(pendingPrimaryColor));
        y += 11;

        addRenderableWidget(new ToggleSwitch(this.font, contentX, y, width, "Aviso de proximo jefe",
                pendingShowTotal, v -> pendingShowTotal = v));
        y += 14;

        addRenderableWidget(new ToggleSwitch(this.font, contentX, y, width, "Tiempo jugado (sesion)",
                pendingShowPlaytime, v -> pendingShowPlaytime = v));
        y += 14;

        addRenderableWidget(new ToggleSwitch(this.font, contentX, y, width, "Fragmentos de Zafiro obtenidos",
                pendingShowFragments, v -> pendingShowFragments = v));
        y += 14;

        addRenderableWidget(new ToggleSwitch(this.font, contentX, y, width, "FPS",
                pendingShowFps, v -> pendingShowFps = v));
        y += 14;

        addRenderableWidget(new ToggleSwitch(this.font, contentX, y, width, "Ping",
                pendingShowPing, v -> pendingShowPing = v));
        y += 14;

        addRenderableWidget(new ToggleSwitch(this.font, contentX, y, width, "Coordenadas",
                pendingShowCoords, v -> pendingShowCoords = v));
        y += 20;

        addLabel("Los jefes derrotados siempre se muestran; usa", contentX, y, COLOR_HINT);
        y += 9;
        addLabel("\"Jefes\" para ocultar alguno.", contentX, y, COLOR_HINT);

        buildPreview();
    }

    // -----------------------------------------------------------------
    // Pestaña: Jefes
    // -----------------------------------------------------------------

    /** A que grupo pertenece un jefe: "MUNDO" si su drop es el mismo (sin grupo
     *  compartido), o el nombre del grupo ("CRISTAL DE OLYMPIUM" / "FRAGMENTO
     *  DE ZAFIRO") si varios jefes comparten ese mismo drop. */
    private String bossGroupOf(String bossName) {
        String drop = config.customBosses.get(bossName);
        if (drop == null || drop.equals(bossName)) {
            return GROUP_WORLD;
        }
        return drop;
    }

    private List<String> jefesNamesForCurrentFilter() {
        List<String> all = new ArrayList<>(config.customBosses.keySet());
        if (GROUP_ALL.equals(jefesGroupFilter)) {
            return all;
        }
        List<String> filtered = new ArrayList<>();
        for (String name : all) {
            if (bossGroupOf(name).equals(jefesGroupFilter)) {
                filtered.add(name);
            }
        }
        return filtered;
    }

    private void buildJefesTab() {
        int y = panelTop + HEADER_HEIGHT;
        int width = currentContentWidth();

        List<String> allNames = new ArrayList<>(config.customBosses.keySet());
        int worldCount = 0, crystalCount = 0, sapphireCount = 0;
        for (String name : allNames) {
            String group = bossGroupOf(name);
            if (group.equals(GROUP_WORLD)) worldCount++;
            else if (group.equals("CRISTAL DE OLYMPIUM")) crystalCount++;
            else if (group.equals("FRAGMENTO DE ZAFIRO")) sapphireCount++;
        }

        // --- Fila de filtros por grupo: en vez de siempre ver los 33 jefes
        // uno por uno, un clic reduce la lista a solo ese grupo. ---
        int filterGap = 4;
        int filterWidth = (width - filterGap * 3) / 4;
        addGroupFilterButton("Todos (" + allNames.size() + ")", GROUP_ALL, contentX, y, filterWidth);
        addGroupFilterButton("Mundo (" + worldCount + ")", GROUP_WORLD, contentX + (filterWidth + filterGap), y, filterWidth);
        addGroupFilterButton("Cristal (" + crystalCount + ")", "CRISTAL DE OLYMPIUM", contentX + (filterWidth + filterGap) * 2, y, filterWidth);
        addGroupFilterButton("Zafiro (" + sapphireCount + ")", "FRAGMENTO DE ZAFIRO", contentX + (filterWidth + filterGap) * 3, y, filterWidth);
        y += 17;

        List<String> names = jefesNamesForCurrentFilter();
        int totalPages = Math.max(1, (int) Math.ceil(names.size() / (double) JEFES_ROWS_PER_PAGE));
        jefesPage = Math.max(0, Math.min(jefesPage, totalPages - 1));

        addLabel("JEFES (" + names.size() + ") \u00b7 PAG " + (jefesPage + 1) + "/" + totalPages,
                contentX, y, HudTheme.colorAt(pendingPrimaryColor));
        y += 10;

        // --- Acciones en bloque: aplican a TODOS los jefes del filtro
        // actual de una sola vez (si el filtro es "Todos", aplica a los 33). ---
        int bulkGap = 4;
        int bulkWidth = (width - bulkGap * 2) / 3;
        addRenderableWidget(Button.builder(Component.literal("Activar grupo"), b -> bulkSetEnabled(true))
                .bounds(contentX, y, bulkWidth, 14)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Desactivar grupo"), b -> bulkSetEnabled(false))
                .bounds(contentX + bulkWidth + bulkGap, y, bulkWidth, 14)
                .build());
        String groupPriorityLabel = names.isEmpty() ? "Prioridad" : HudTheme.PRIORITY_NAMES[pendingBossPriority.getOrDefault(names.get(0), 1)];
        addRenderableWidget(Button.builder(Component.literal(groupPriorityLabel), b -> bulkCyclePriority())
                .bounds(contentX + (bulkWidth + bulkGap) * 2, y, bulkWidth, 14)
                .build());
        y += 18;

        int halfWidth = (width - 8) / 2;
        addRenderableWidget(Button.builder(
                        Component.literal("Orden: " + HudTheme.SORT_MODE_NAMES[pendingSortMode]),
                        b -> { pendingSortMode = (pendingSortMode + 1) % HudTheme.SORT_MODE_NAMES.length; switchTab(Tab.JEFES); })
                .bounds(contentX, y, halfWidth, 14)
                .build());
        addRenderableWidget(new GalyxSlider(contentX + halfWidth + 8, y, halfWidth, 14, 1, 40, 1, pendingMaxBossesShown,
                v -> "Max en HUD: " + (int) v, v -> pendingMaxBossesShown = (int) v));
        y += 17;

        int colorX = contentX + width - 88;
        int priorityX = colorX + 34;

        addLabel("Color", colorX, y + 6, COLOR_HINT);
        addLabel("Prioridad", priorityX, y + 6, COLOR_HINT);
        y += 14;

        int start = jefesPage * JEFES_ROWS_PER_PAGE;
        int end = Math.min(start + JEFES_ROWS_PER_PAGE, names.size());

        for (int i = start; i < end; i++) {
            String bossName = names.get(i);
            buildBossRow(bossName, contentX, y, width, colorX, priorityX);
            y += 18;
        }

        y += 2;
        addRenderableWidget(Button.builder(Component.literal("< Ant."), b -> {
                    if (jefesPage > 0) { jefesPage--; switchTab(Tab.JEFES); }
                })
                .bounds(contentX, y, 66, 15)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Sig. >"), b -> {
                    if (jefesPage < totalPages - 1) { jefesPage++; switchTab(Tab.JEFES); }
                })
                .bounds(contentX + width - 66, y, 66, 15)
                .build());
        y += 18;

        addRenderableWidget(Button.builder(
                        Component.literal("Editar nombres / confirmaciones"),
                        b -> Minecraft.getInstance().setScreen(new BossListScreen(config, configManager)))
                .bounds(contentX, y, width, 15)
                .build());
    }

    private void addGroupFilterButton(String label, String groupId, int x, int y, int width) {
        boolean active = jefesGroupFilter.equals(groupId);
        addRenderableWidget(Button.builder(
                        Component.literal((active ? "\u25cf " : "") + label),
                        b -> { jefesGroupFilter = groupId; jefesPage = 0; switchTab(Tab.JEFES); })
                .bounds(x, y, width, 14)
                .build());
    }

    private void bulkSetEnabled(boolean enabled) {
        for (String name : jefesNamesForCurrentFilter()) {
            pendingBossEnabled.put(name, enabled);
        }
        switchTab(Tab.JEFES);
    }

    private void bulkCyclePriority() {
        List<String> names = jefesNamesForCurrentFilter();
        if (names.isEmpty()) {
            return;
        }
        int current = pendingBossPriority.getOrDefault(names.get(0), 1);
        int next = (current + 1) % HudTheme.PRIORITY_NAMES.length;
        for (String name : names) {
            pendingBossPriority.put(name, next);
        }
        switchTab(Tab.JEFES);
    }

    private void buildBossRow(String bossName, int x, int y, int width, int colorX, int priorityX) {
        boolean enabled = pendingBossEnabled.getOrDefault(bossName, true);
        int colorIndex = pendingBossColor.getOrDefault(bossName, -1);
        int priority = pendingBossPriority.getOrDefault(bossName, 1);

        addRenderableWidget(new ToggleSwitch(this.font, x, y, 26, "", enabled,
                v -> pendingBossEnabled.put(bossName, v)));

        String displayName = truncate(bossName, colorX - (x + 30));
        addLabel(displayName, x + 30, y + 3, enabled ? COLOR_LABEL : COLOR_HINT);

        SwatchButton swatch = new SwatchButton(colorX, y, 12,
                colorIndex < 0 ? 0xFF55555C : HudTheme.colorAt(colorIndex),
                bossName + " color", false, s -> {
            int next = colorIndex + 1;
            if (next >= HudTheme.BORDER_COLORS.length) {
                next = -1;
            }
            pendingBossColor.put(bossName, next);
            switchTab(Tab.JEFES);
        });
        if (colorIndex < 0) {
            swatch.setGlyph(this.font, "A");
        }
        addRenderableWidget(swatch);

        addRenderableWidget(Button.builder(
                        Component.literal(HudTheme.PRIORITY_NAMES[priority]),
                        b -> {
                            int next = (priority + 1) % HudTheme.PRIORITY_NAMES.length;
                            pendingBossPriority.put(bossName, next);
                            switchTab(Tab.JEFES);
                        })
                .bounds(priorityX, y - 1, x + width - priorityX, 14)
                .build());
    }

    private String truncate(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        String result = text;
        while (result.length() > 1 && this.font.width(result + "...") > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "...";
    }

    // -----------------------------------------------------------------
    // Pestaña: Notificaciones
    // -----------------------------------------------------------------

    private void buildNotificacionesTab() {
        int y = panelTop + HEADER_HEIGHT;
        int width = currentContentWidth();

        addLabel("AVISOS DE SPAWN", contentX, y, HudTheme.colorAt(pendingPrimaryColor));
        y += 11;

        addRenderableWidget(new ToggleSwitch(this.font, contentX, y, width, "Sonido en avisos",
                pendingSoundEnabled, v -> pendingSoundEnabled = v));
        y += 14;

        addRenderableWidget(new ToggleSwitch(this.font, contentX, y, width, "Mostrar en chat (no action bar)",
                pendingUseChat, v -> pendingUseChat = v));
        y += 20;

        addLabel("Minutos de aviso: " + pendingWarningMinutes, contentX, y, COLOR_LABEL);
        y += 9;
        addRenderableWidget(new GalyxSlider(contentX, y, width, 14, 1, 30, 1, pendingWarningMinutes,
                v -> (int) v + " min antes", v -> pendingWarningMinutes = (int) v));
        y += 20;

        addLabel("Aplica a los 27 jefes de /boss con horario", contentX, y, COLOR_HINT);
        y += 9;
        addLabel("configurado en bossSpawnTimes.", contentX, y, COLOR_HINT);
    }

    // -----------------------------------------------------------------
    // Pestaña: Avanzado
    // -----------------------------------------------------------------

    private void buildAvanzadoTab() {
        int y = panelTop + HEADER_HEIGHT;
        int width = currentContentWidth();

        addLabel("RENDIMIENTO", contentX, y, HudTheme.colorAt(pendingPrimaryColor));
        y += 11;

        addLabel("Actualizar HUD cada:", contentX, y, COLOR_LABEL);
        y += 9;
        addRenderableWidget(Button.builder(
                        Component.literal(intervalLabel(pendingUpdateIntervalTicks)),
                        b -> {
                            int idx = indexOfInterval(pendingUpdateIntervalTicks);
                            idx = (idx + 1) % UPDATE_INTERVALS.length;
                            pendingUpdateIntervalTicks = UPDATE_INTERVALS[idx];
                            switchTab(Tab.AVANZADO);
                        })
                .bounds(contentX, y, width, 16)
                .build());
        y += 22;

        addLabel("EXPORTAR / IMPORTAR", contentX, y, HudTheme.colorAt(pendingPrimaryColor));
        y += 11;

        addRenderableWidget(Button.builder(Component.literal("Exportar"), b -> exportConfig())
                .bounds(contentX, y, width / 2 - 3, 16)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Importar"), b -> importConfig())
                .bounds(contentX + width / 2 + 3, y, width / 2 - 3, 16)
                .build());
        y += 19;

        addLabel("Importar no reemplaza tu lista de jefes,", contentX, y, COLOR_HINT);
        y += 9;
        addLabel("solo el estilo y comportamiento del HUD.", contentX, y, COLOR_HINT);
        y += 18;

        addLabel("ZONA DE RIESGO", contentX, y, HudTheme.colorAt(pendingPrimaryColor));
        y += 11;
        addRenderableWidget(Button.builder(Component.literal("Restablecer a valores por defecto"), b -> resetToDefaults())
                .bounds(contentX, y, width, 16)
                .build());
    }

    private String intervalLabel(int ticks) {
        return ticks <= 1 ? "Cada tick" : "Cada " + ticks + " ticks";
    }

    private int indexOfInterval(int ticks) {
        for (int i = 0; i < UPDATE_INTERVALS.length; i++) {
            if (UPDATE_INTERVALS[i] == ticks) {
                return i;
            }
        }
        return 0;
    }

    private void exportConfig() {
        GalyxConfig snapshot = new GalyxConfig();
        applyPendingTo(snapshot);
        snapshot.customBosses = config.customBosses;
        snapshot.bossSpawnTimes = config.bossSpawnTimes;
        snapshot.bossEnabled = new LinkedHashMap<>(pendingBossEnabled);
        snapshot.bossPriority = new LinkedHashMap<>(pendingBossPriority);
        snapshot.bossColorIndex = new LinkedHashMap<>(pendingBossColor);
        snapshot.bossHeadNames = new LinkedHashMap<>(pendingBossHeadNames);

        String json = GSON.toJson(snapshot);
        this.minecraft.keyboardHandler.setClipboard(json);
        statusMessage = "Configuracion copiada al portapapeles";
    }

    private void importConfig() {
        String clipboard = this.minecraft.keyboardHandler.getClipboard();
        if (clipboard == null || clipboard.isBlank()) {
            statusMessage = "El portapapeles esta vacio";
            return;
        }
        try {
            GalyxConfig parsed = GSON.fromJson(clipboard, GalyxConfig.class);
            if (parsed == null) {
                statusMessage = "No se pudo leer la configuracion";
                return;
            }
            pendingHudEnabled = parsed.hudEnabled;
            pendingBossDetector = parsed.bossDetectorEnabled;
            pendingTimezone = parsed.spawnNotifierTimezone != null ? parsed.spawnNotifierTimezone : pendingTimezone;
            pendingHudX = parsed.hudX;
            pendingHudY = parsed.hudY;
            pendingHudScale = parsed.hudScale;
            pendingUseIcons = parsed.hudUseIcons;
            pendingShowTotal = parsed.hudShowTotal;
            pendingIconSize = parsed.hudIconSize;
            pendingPanelAlpha = parsed.hudPanelAlpha;
            pendingPrimaryColor = clampIndex(parsed.hudBorderColorIndex);
            pendingSecondaryColor = clampIndex(parsed.hudSecondaryColorIndex);
            pendingRounded = parsed.hudRoundedCorners;
            pendingShowBorder = parsed.hudShowBorder;
            pendingAnchor = clampAnchor(parsed.hudAnchor);
            pendingBackgroundColor = parsed.hudBackgroundColorIndex;
            pendingTextShadow = parsed.hudTextShadow;
            pendingShowPlaytime = parsed.hudShowPlaytime;
            pendingShowFragments = parsed.hudShowFragments;
            pendingShowFps = parsed.hudShowFps;
            pendingShowPing = parsed.hudShowPing;
            pendingShowCoords = parsed.hudShowCoords;
            pendingSortMode = clampSortMode(parsed.hudSortMode);
            pendingMaxBossesShown = Math.max(1, parsed.hudMaxBossesShown);
            pendingUpdateIntervalTicks = parsed.hudUpdateIntervalTicks;
            pendingSoundEnabled = parsed.spawnNotifierSoundEnabled;
            pendingUseChat = parsed.notifyUseChat;
            pendingWarningMinutes = Math.max(1, parsed.notifyWarningMinutes);
            if (parsed.bossHeadNames != null && !parsed.bossHeadNames.isEmpty()) {
                pendingBossHeadNames.clear();
                pendingBossHeadNames.putAll(parsed.bossHeadNames);
            }

            statusMessage = "Configuracion importada, pulsa Guardar para aplicar";
            switchTab(selectedTab);
        } catch (JsonSyntaxException e) {
            statusMessage = "El portapapeles no contiene un JSON valido";
        }
    }

    private int clampIndex(int index) {
        return Math.max(0, Math.min(HudTheme.BORDER_COLORS.length - 1, index));
    }

    private int clampAnchor(int anchor) {
        return Math.max(0, Math.min(HudTheme.ANCHOR_NAMES.length - 1, anchor));
    }

    private int clampSortMode(int sortMode) {
        return Math.max(0, Math.min(HudTheme.SORT_MODE_NAMES.length - 1, sortMode));
    }

    private int clampBackground(int index) {
        return Math.max(-1, Math.min(HudTheme.BORDER_COLORS.length - 1, index));
    }

    // -----------------------------------------------------------------
    // Pestaña: Informacion
    // -----------------------------------------------------------------

    private void buildInformacionTab() {
        int y = panelTop + HEADER_HEIGHT;

        String version = FabricLoader.getInstance()
                .getModContainer(GALYX.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("desconocida");
        String username = Minecraft.getInstance().getUser() != null
                ? Minecraft.getInstance().getUser().getName()
                : "desconocido";

        addLabel("GALYX v" + version, contentX, y, HudTheme.colorAt(pendingPrimaryColor));
        y += 14;
        addLabel("Guardian Ancestral de Leyendas Y eXpediciones", contentX, y, COLOR_HINT);
        y += 16;

        addLabel("Minecraft: 1.21.11 (Fabric)", contentX, y, COLOR_LABEL); y += 12;
        addLabel("Java: " + System.getProperty("java.version", "desconocido"), contentX, y, COLOR_LABEL); y += 12;
        addLabel("Servidor objetivo: OlympoMC", contentX, y, COLOR_LABEL); y += 12;
        addLabel("Usuario: " + username, contentX, y, COLOR_LABEL); y += 12;
        addLabel("Jefes registrados: " + config.customBosses.size(), contentX, y, COLOR_LABEL); y += 12;
        addLabel("Horarios de spawn configurados: " + config.bossSpawnTimes.size(), contentX, y, COLOR_LABEL);
        y += 18;

        buildHeadCounterSection(y);
    }

    // -----------------------------------------------------------------
    // Cabezas de jefes de mundo en el inventario
    // -----------------------------------------------------------------

    private void buildHeadCounterSection(int y) {
        int width = currentContentWidth();

        addLabel("CABEZAS EN INVENTARIO (mundo)", contentX, y, HudTheme.colorAt(pendingPrimaryColor));
        y += 11;

        Map<String, Integer> liveCounts = com.galyx.client.stats.InventoryHeadCounter.count(pendingBossHeadNames);
        int total = 0;
        for (int v : liveCounts.values()) {
            total += v;
        }

        if (!editingHeadNames) {
            for (Map.Entry<String, String> entry : pendingBossHeadNames.entrySet()) {
                String bossName = entry.getKey();
                int count = liveCounts.getOrDefault(bossName, 0);
                addLabel(bossName + ": " + count, contentX, y, COLOR_LABEL);
                y += 10;
            }
            y += 2;
            addLabel("Total: " + total + " cabezas en inventario", contentX, y, COLOR_SUBTITLE);
            y += 14;

            addRenderableWidget(Button.builder(Component.literal("Editar nombres de las cabezas"),
                            b -> { editingHeadNames = true; switchTab(Tab.INFORMACION); })
                    .bounds(contentX, y, width, 15)
                    .build());
        } else {
            addLabel("Nombre exacto tal como aparece en el tooltip:", contentX, y, COLOR_HINT);
            y += 10;

            for (Map.Entry<String, String> entry : pendingBossHeadNames.entrySet()) {
                String bossName = entry.getKey();
                addLabel(bossName, contentX, y + 2, COLOR_HINT);
                EditBox nameBox = new EditBox(this.font, contentX, y + 10, width, 14, Component.literal("Nombre de cabeza"));
                nameBox.setMaxLength(64);
                nameBox.setValue(entry.getValue());
                nameBox.setResponder(text -> pendingBossHeadNames.put(bossName, text));
                addRenderableWidget(nameBox);
                y += 26;
            }

            addRenderableWidget(Button.builder(Component.literal("Listo"),
                            b -> { editingHeadNames = false; switchTab(Tab.INFORMACION); })
                    .bounds(contentX, y, width, 15)
                    .build());
        }
    }

    // -----------------------------------------------------------------
    // Vista previa en vivo del HUD
    // -----------------------------------------------------------------

    private void buildPreview() {
        // La vista previa se dibuja en render(); aqui solo reservamos el
        // area (previewX/previewY/previewWidth/previewHeight ya calculados
        // en init()) para que mouseClicked/mouseDragged sepan si el cursor
        // esta dentro cuando "Mover HUD" esta activo.
    }

    private boolean isInsidePreview(double mouseX, double mouseY) {
        return previewVisible
                && mouseX >= previewX && mouseX < previewX + previewWidth
                && mouseY >= previewY && mouseY < previewY + previewHeight;
    }

    private void renderPreviewPanel(GuiGraphics context, int mouseX, int mouseY) {
        if (!previewVisible) {
            return;
        }

        context.fill(previewX, previewY, previewX + previewWidth, previewY + previewHeight, COLOR_PREVIEW_BG);
        context.hLine(previewX, previewX + previewWidth - 1, previewY, 0xFF2A2A34);
        context.hLine(previewX, previewX + previewWidth - 1, previewY + previewHeight - 1, 0xFF2A2A34);
        context.vLine(previewX, previewY, previewY + previewHeight - 1, 0xFF2A2A34);
        context.vLine(previewX + previewWidth - 1, previewY, previewY + previewHeight - 1, 0xFF2A2A34);

        context.drawCenteredString(this.font, "[ PREVISUALIZAR HUD ]", previewX + previewWidth / 2, previewY + 6, COLOR_SUBTITLE);

        boolean hoveringPreview = selectedTab == Tab.APARIENCIA
                && mouseX >= previewX && mouseX < previewX + previewWidth
                && mouseY >= previewY && mouseY < previewY + previewHeight
                && !dragArmed;
        if (hoveringPreview) {
            // Mismo hint que en el mockup: pasar el mouse por la vista previa
            // recuerda que "Mover HUD" (columna izquierda) es lo que la
            // vuelve arrastrable -- aqui solo es un indicador visual, el
            // clic en si sigue sin hacer nada hasta activar ese boton.
            context.drawString(this.font, "Editar \u2192", mouseX + 10, mouseY - 4, HudTheme.colorAt(pendingPrimaryColor), true);
        }

        Font font = this.font;
        int primary = HudTheme.colorAt(pendingPrimaryColor);
        int secondary = HudTheme.colorAt(pendingSecondaryColor);
        int panelBg = (pendingPanelAlpha << 24) | 0x101018;
        int borderColor = (0x60 << 24) | (primary & 0xFFFFFF);
        int separatorColor = (0x80 << 24) | (primary & 0xFFFFFF);

        // Datos de muestra: la vista previa no tiene acceso a las
        // estadisticas reales (esta pantalla se abre sin StatisticsManager),
        // asi que usa valores ilustrativos para mostrar el estilo.
        List<String[]> sampleLines = new ArrayList<>();
        sampleLines.add(new String[]{"REY NEPTUNO", "3"});
        sampleLines.add(new String[]{"CLEOPATRA", "1"});

        List<String> extraLines = new ArrayList<>();
        if (pendingShowPlaytime) extraLines.add("Tiempo jugado: 2h 15m");
        if (pendingShowFragments) extraLines.add("Fragmentos: 12");
        if (pendingShowFps) extraLines.add("FPS: " + Minecraft.getInstance().getFps());
        if (pendingShowPing) extraLines.add("Ping: 42 ms");
        if (pendingShowCoords) extraLines.add("XYZ: 120 / 64 / -430");

        float scale = Math.max(0.6f, Math.min(1.0f, pendingHudScale));
        int iconSize = Math.max(4, Math.min(20, pendingIconSize));

        int boxX = previewX + 14;
        int boxYStart = previewY + 22;
        int lineHeight = 12;

        int maxWidth = font.width("GALYX");
        maxWidth = Math.max(maxWidth, font.width("Total: 4 derrotados"));
        for (String[] line : sampleLines) {
            maxWidth = Math.max(maxWidth, font.width(line[0] + ": " + line[1]));
        }
        for (String line : extraLines) {
            maxWidth = Math.max(maxWidth, font.width(line));
        }
        maxWidth = Math.min(maxWidth, previewWidth - 26);

        int lineCount = sampleLines.size() + 1 + extraLines.size() + (pendingShowTotal ? 1 : 0);
        int boxWidth = maxWidth + 12;
        int boxHeight = lineHeight + lineCount * lineHeight + 12;

        context.fill(boxX, boxYStart, boxX + boxWidth, boxYStart + boxHeight, panelBg);
        if (pendingShowBorder) {
            context.hLine(boxX, boxX + boxWidth - 1, boxYStart, borderColor);
            context.hLine(boxX, boxX + boxWidth - 1, boxYStart + boxHeight - 1, borderColor);
            context.vLine(boxX, boxYStart, boxYStart + boxHeight - 1, borderColor);
            context.vLine(boxX + boxWidth - 1, boxYStart, boxYStart + boxHeight - 1, borderColor);
        }
        if (pendingRounded) {
            drawCorner(context, boxX, boxYStart, 1, 1, primary);
            drawCorner(context, boxX + boxWidth, boxYStart, -1, 1, primary);
            drawCorner(context, boxX, boxYStart + boxHeight, 1, -1, primary);
            drawCorner(context, boxX + boxWidth, boxYStart + boxHeight, -1, -1, primary);
        }

        int textX = boxX + 6;
        int textY = boxYStart + 6;
        context.drawString(font, "GALYX", textX, textY, primary, true);
        textY += lineHeight;
        context.hLine(textX, textX + maxWidth, textY - 2, separatorColor);

        for (String[] line : sampleLines) {
            if (pendingUseIcons) {
                context.fill(textX, textY - 1, textX + iconSize, textY - 1 + iconSize, 0x60FFFFFF);
                context.drawString(font, line[1], textX + iconSize + 4, textY, secondary, true);
            } else {
                context.drawString(font, line[0] + ": ", textX, textY, primary, true);
                context.drawString(font, line[1], textX + font.width(line[0] + ": "), textY, secondary, true);
            }
            textY += lineHeight;
        }

        context.hLine(textX, textX + maxWidth, textY - 2, separatorColor);
        context.drawString(font, "Total: 4 derrotados", textX, textY, 0xFFFFFFFF, true);
        textY += lineHeight;

        if (pendingShowTotal) {
            context.hLine(textX, textX + maxWidth, textY - 2, separatorColor);
            context.drawString(font, "Prox. jefe: en 34m", textX, textY, secondary, true);
            textY += lineHeight;
        }

        for (String line : extraLines) {
            context.drawString(font, line, textX, textY, COLOR_LABEL, true);
            textY += lineHeight;
        }

        int hintY = previewY + previewHeight - 10;
        if (dragArmed) {
            context.drawCenteredString(this.font, "Arrastra para mover", previewX + previewWidth / 2, hintY, HudTheme.colorAt(pendingPrimaryColor));
        } else {
            context.drawCenteredString(this.font, "X:" + pendingHudX + " Y:" + pendingHudY + " x" + String.format("%.2f", pendingHudScale),
                    previewX + previewWidth / 2, hintY, COLOR_HINT);
        }
    }

    private int currentContentWidth() {
        return contentWidth;
    }

    // -----------------------------------------------------------------
    // Render
    // -----------------------------------------------------------------

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(context);

        int centerX = this.width / 2;
        int themeColor = HudTheme.colorAt(pendingPrimaryColor);

        int outerLeft = panelLeft - PANEL_MARGIN;
        int outerTop = panelTop - PANEL_MARGIN;
        int outerRight = panelLeft + panelWidth + PANEL_MARGIN;
        int outerBottom = panelTop + panelHeight;

        context.fillGradient(outerLeft, outerTop, outerRight, outerBottom, COLOR_PANEL_BG_TOP, COLOR_PANEL_BG_BOTTOM);
        context.hLine(outerLeft, outerRight - 1, outerTop, themeColor);
        context.hLine(outerLeft, outerRight - 1, outerBottom - 1, themeColor);
        context.vLine(outerLeft, outerTop, outerBottom - 1, themeColor);
        context.vLine(outerRight - 1, outerTop, outerBottom - 1, themeColor);
        drawCorner(context, outerLeft, outerTop, 1, 1, themeColor);
        drawCorner(context, outerRight, outerTop, -1, 1, themeColor);
        drawCorner(context, outerLeft, outerBottom, 1, -1, themeColor);
        drawCorner(context, outerRight, outerBottom, -1, -1, themeColor);

        context.fill(sidebarX - 4, panelTop + HEADER_HEIGHT - 4,
                sidebarX + SIDEBAR_WIDTH + 4, panelTop + HEADER_HEIGHT + bodyHeight, COLOR_SIDEBAR_BG);

        int logoSize = 14;
        int titleWidth = this.font.width("GALYX");
        int headerWidth = logoSize + 5 + titleWidth;
        int headerLeft = centerX - headerWidth / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, LOGO, headerLeft, outerTop + 3, 0, 0, logoSize, logoSize, logoSize, logoSize);
        context.drawString(this.font, "GALYX", headerLeft + logoSize + 5, outerTop + 3 + logoSize / 2 - 4, themeColor, true);
        context.drawCenteredString(this.font, "OlympoMC", centerX, outerTop + 3 + logoSize + 3, COLOR_SUBTITLE);

        int sepY = outerTop + 3 + logoSize + 12;
        context.hLine(panelLeft, panelLeft + panelWidth, sepY, themeColor);

        for (Label label : labels) {
            context.drawString(this.font, label.text, label.x, label.y, label.color, false);
        }

        renderPreviewPanel(context, mouseX, mouseY);

        super.render(context, mouseX, mouseY, partialTick);

        int footerTextY = panelTop + panelHeight - FOOTER_HEIGHT - 10;
        if (!statusMessage.isEmpty()) {
            boolean ok = statusMessage.startsWith("Guardado") || statusMessage.contains("copiada") || statusMessage.contains("importada");
            int color = ok ? COLOR_STATUS_OK : COLOR_STATUS_ERR;
            context.drawCenteredString(this.font, statusMessage, centerX, footerTextY, color);
        } else if (isDirty()) {
            context.drawCenteredString(this.font, "Hay cambios sin guardar", centerX, footerTextY, 0xFFF0C050);
        } else {
            String estado = "GALYX activo \u2022 OlympoMC \u2022 HUD " + (pendingHudEnabled ? "habilitado" : "deshabilitado");
            context.drawCenteredString(this.font, estado, centerX, footerTextY, COLOR_HINT);
        }
    }

    private void drawCorner(GuiGraphics context, int x, int y, int dirX, int dirY, int color) {
        context.hLine(x, x + dirX * CORNER_SIZE, y, color);
        context.vLine(x, y, y + dirY * CORNER_SIZE, color);
    }

    // -----------------------------------------------------------------
    // Arrastrar la vista previa (reposicionar el HUD)
    // -----------------------------------------------------------------

    //? if >=1.21.9 {
    /*
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (dragArmed && isInsidePreview(event.x(), event.y())) {
            dragging = true;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging) {
            pendingHudX += (int) Math.round(dragX);
            pendingHudY += (int) Math.round(dragY);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging) {
            dragging = false;
            // Los campos X/Y de "Posicion y escala" son texto fijo (steppers),
            // no un slider que se auto-actualiza -- hace falta reconstruir la
            // pestaña una vez al soltar el clic para que muestren el valor nuevo.
            switchTab(selectedTab);
            return true;
        }
        return super.mouseReleased(event);
    }
    */
    //?} else {
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (dragArmed && isInsidePreview(mouseX, mouseY)) {
            dragging = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            pendingHudX += (int) Math.round(dragX);
            pendingHudY += (int) Math.round(dragY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            switchTab(selectedTab);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    //?}

    // -----------------------------------------------------------------
    // Guardar / restablecer
    // -----------------------------------------------------------------

    private void save() {
        String enteredZone = pendingTimezone == null ? "" : pendingTimezone.trim();
        try {
            ZoneId.of(enteredZone);
        } catch (Exception e) {
            statusMessage = "Zona horaria invalida, no se guardo";
            return;
        }

        applyPendingTo(config);
        config.bossEnabled.clear();
        config.bossEnabled.putAll(pendingBossEnabled);
        config.bossPriority.clear();
        config.bossPriority.putAll(pendingBossPriority);
        config.bossColorIndex.clear();
        config.bossColorIndex.putAll(pendingBossColor);
        config.bossHeadNames.clear();
        config.bossHeadNames.putAll(pendingBossHeadNames);

        configManager.save();
        statusMessage = "Guardado";
    }

    private void applyPendingTo(GalyxConfig target) {
        target.hudEnabled = pendingHudEnabled;
        target.bossDetectorEnabled = pendingBossDetector;
        target.spawnNotifierTimezone = pendingTimezone == null ? target.spawnNotifierTimezone : pendingTimezone.trim();

        target.hudX = pendingHudX;
        target.hudY = pendingHudY;
        target.hudScale = pendingHudScale;

        target.hudUseIcons = pendingUseIcons;
        target.hudShowTotal = pendingShowTotal;
        target.hudIconSize = clamp(pendingIconSize, 4, 64);
        target.hudPanelAlpha = clamp(pendingPanelAlpha, 0, 255);
        target.hudBorderColorIndex = clampIndex(pendingPrimaryColor);
        target.hudSecondaryColorIndex = clampIndex(pendingSecondaryColor);
        target.hudRoundedCorners = pendingRounded;
        target.hudShowBorder = pendingShowBorder;
        target.hudAnchor = clampAnchor(pendingAnchor);
        target.hudBackgroundColorIndex = clampBackground(pendingBackgroundColor);
        target.hudTextShadow = pendingTextShadow;

        target.hudShowPlaytime = pendingShowPlaytime;
        target.hudShowFragments = pendingShowFragments;
        target.hudShowFps = pendingShowFps;
        target.hudShowPing = pendingShowPing;
        target.hudShowCoords = pendingShowCoords;

        target.hudSortMode = clampSortMode(pendingSortMode);
        target.hudMaxBossesShown = clamp(pendingMaxBossesShown, 1, 40);

        target.hudUpdateIntervalTicks = pendingUpdateIntervalTicks;

        target.spawnNotifierSoundEnabled = pendingSoundEnabled;
        target.notifyUseChat = pendingUseChat;
        target.notifyWarningMinutes = clamp(pendingWarningMinutes, 1, 30);
    }

    private void resetToDefaults() {
        loadPendingFromDefaults();
        statusMessage = "Valores por defecto cargados, pulsa Guardar para confirmar";
        switchTab(selectedTab);
    }

    private void loadPendingFromDefaults() {
        GalyxConfig defaults = new GalyxConfig();
        pendingHudEnabled = defaults.hudEnabled;
        pendingBossDetector = defaults.bossDetectorEnabled;
        pendingTimezone = defaults.spawnNotifierTimezone;
        pendingHudX = defaults.hudX;
        pendingHudY = defaults.hudY;
        pendingHudScale = defaults.hudScale;
        pendingUseIcons = defaults.hudUseIcons;
        pendingShowTotal = defaults.hudShowTotal;
        pendingIconSize = defaults.hudIconSize;
        pendingPanelAlpha = defaults.hudPanelAlpha;
        pendingPrimaryColor = defaults.hudBorderColorIndex;
        pendingSecondaryColor = defaults.hudSecondaryColorIndex;
        pendingRounded = defaults.hudRoundedCorners;
        pendingShowBorder = defaults.hudShowBorder;
        pendingAnchor = defaults.hudAnchor;
        pendingBackgroundColor = defaults.hudBackgroundColorIndex;
        pendingTextShadow = defaults.hudTextShadow;
        pendingShowPlaytime = defaults.hudShowPlaytime;
        pendingShowFragments = defaults.hudShowFragments;
        pendingShowFps = defaults.hudShowFps;
        pendingShowPing = defaults.hudShowPing;
        pendingShowCoords = defaults.hudShowCoords;
        pendingSortMode = defaults.hudSortMode;
        pendingMaxBossesShown = defaults.hudMaxBossesShown;
        pendingUpdateIntervalTicks = defaults.hudUpdateIntervalTicks;
        pendingSoundEnabled = defaults.spawnNotifierSoundEnabled;
        pendingUseChat = defaults.notifyUseChat;
        pendingWarningMinutes = defaults.notifyWarningMinutes;
        pendingBossEnabled.replaceAll((k, v) -> true);
        pendingBossPriority.replaceAll((k, v) -> 1);
        pendingBossColor.replaceAll((k, v) -> -1);
        pendingBossHeadNames.clear();
        pendingBossHeadNames.putAll(defaults.bossHeadNames);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void addLabel(String text, int x, int y, int color) {
        labels.add(new Label(text, x, y, color));
    }

    private record Label(String text, int x, int y, int color) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parentScreen);
    }

}