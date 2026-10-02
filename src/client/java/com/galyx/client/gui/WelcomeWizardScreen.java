package com.galyx.client.gui;

import com.galyx.client.config.ConfigManager;
import com.galyx.client.config.GalyxConfig;
import com.galyx.client.gui.widget.ToggleSwitch;
import com.galyx.client.hud.HudTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Asistente de primera vez. Se abre solo una vez (ver GalyxConfig#onboardingCompleted
 * y ConfigManager#load) para que alguien nuevo no tenga que entender las 7
 * pestañas de ConfigScreen de entrada -- 4 pasos cortos y ya puede jugar.
 * El resto de las opciones siguen disponibles despues en /galyx cuando quiera.
 */
public class WelcomeWizardScreen extends Screen {

    private static final String[] TIMEZONE_PRESET_IDS = {
            "America/Bogota", "America/Lima", "America/Mexico_City", "America/Santiago",
            "America/Argentina/Buenos_Aires", "America/Caracas", "Europe/Madrid"
    };
    private static final String[] TIMEZONE_PRESET_LABELS = {
            "Colombia", "Peru", "Mexico", "Chile", "Argentina", "Venezuela", "Espana"
    };

    private static final int COLOR_TITLE = HudTheme.colorAt(0);
    private static final int COLOR_TEXT = 0xFFE8E8E8;
    private static final int COLOR_HINT = 0xFF888890;

    private static final int PANEL_WIDTH = 230;
    private static final int PANEL_HEIGHT = 150;

    private final GalyxConfig config;
    private final ConfigManager configManager;

    private int step = 0;
    private static final int TOTAL_STEPS = 4;

    private String chosenTimezone = "America/Bogota";
    private boolean chosenSound = true;
    private boolean chosenChat = true;

    private int panelLeft;
    private int panelTop;

    public WelcomeWizardScreen(GalyxConfig config, ConfigManager configManager) {
        super(Component.literal("GALYX - Bienvenida"));
        this.config = config;
        this.configManager = configManager;
        // Si el jugador ya tenia algo configurado (por ejemplo corrio el
        // asistente antes, o edito el json a mano), se parte de eso en vez
        // de los valores por defecto.
        if (config.spawnNotifierTimezone != null && !config.spawnNotifierTimezone.isBlank()) {
            this.chosenTimezone = config.spawnNotifierTimezone;
        }
        this.chosenSound = config.spawnNotifierSoundEnabled;
        this.chosenChat = config.notifyUseChat;
    }

    @Override
    protected void init() {
        panelLeft = this.width / 2 - PANEL_WIDTH / 2;
        panelTop = this.height / 2 - PANEL_HEIGHT / 2;

        switch (step) {
            case 0 -> buildStepWelcome();
            case 1 -> buildStepTimezone();
            case 2 -> buildStepNotifications();
            case 3 -> buildStepDone();
        }

        buildNavButtons();
    }

    // -----------------------------------------------------------------
    // Paso 0: bienvenida
    // -----------------------------------------------------------------

    private void buildStepWelcome() {
        // Solo texto, se dibuja en render(). Nada que construir aqui.
    }

    // -----------------------------------------------------------------
    // Paso 1: zona horaria (mismos presets que ConfigScreen)
    // -----------------------------------------------------------------

    private void buildStepTimezone() {
        int y = panelTop + 44;
        int gap = 6;
        int colWidth = (PANEL_WIDTH - 20 - gap) / 2;
        int rowHeight = 15;

        for (int i = 0; i < TIMEZONE_PRESET_IDS.length; i++) {
            String zoneId = TIMEZONE_PRESET_IDS[i];
            boolean selected = zoneId.equals(chosenTimezone);
            int col = i % 2;
            int row = i / 2;
            int x = panelLeft + 10 + col * (colWidth + gap);
            int py = y + row * (rowHeight + 3);
            addRenderableWidget(Button.builder(
                            Component.literal((selected ? "\u25cf " : "\u25cb ") + TIMEZONE_PRESET_LABELS[i]),
                            b -> { chosenTimezone = zoneId; rebuild(); })
                    .bounds(x, py, colWidth, rowHeight)
                    .build());
        }
    }

    // -----------------------------------------------------------------
    // Paso 2: notificaciones
    // -----------------------------------------------------------------

    private void buildStepNotifications() {
        int y = panelTop + 50;
        int width = PANEL_WIDTH - 20;

        addRenderableWidget(new ToggleSwitch(this.font, panelLeft + 10, y, width,
                "Sonido en los avisos", chosenSound, v -> chosenSound = v));
        y += 20;
        addRenderableWidget(new ToggleSwitch(this.font, panelLeft + 10, y, width,
                "Avisar tambien en el chat", chosenChat, v -> chosenChat = v));
    }

    // -----------------------------------------------------------------
    // Paso 3: listo
    // -----------------------------------------------------------------

    private void buildStepDone() {
        // Solo texto + boton "Comenzar" (se agrega en buildNavButtons).
    }

    // -----------------------------------------------------------------
    // Navegacion
    // -----------------------------------------------------------------

    private void buildNavButtons() {
        int navY = panelTop + PANEL_HEIGHT - 22;
        int navWidth = 70;

        if (step > 0 && step < TOTAL_STEPS - 1) {
            addRenderableWidget(Button.builder(Component.literal("\u2190 Atras"), b -> { step--; rebuild(); })
                    .bounds(panelLeft + 10, navY, navWidth, 16)
                    .build());
        }

        if (step < TOTAL_STEPS - 1) {
            addRenderableWidget(Button.builder(Component.literal("Siguiente \u2192"), b -> { step++; rebuild(); })
                    .bounds(panelLeft + PANEL_WIDTH - 10 - navWidth, navY, navWidth, 16)
                    .build());
        }

        if (step == 0) {
            addRenderableWidget(Button.builder(Component.literal("Omitir"), b -> finish())
                    .bounds(panelLeft + 10, navY, navWidth, 16)
                    .build());
        }

        if (step == TOTAL_STEPS - 1) {
            addRenderableWidget(Button.builder(Component.literal("\u00a1Comenzar!"), b -> finish())
                    .bounds(panelLeft + PANEL_WIDTH / 2 - 45, navY, 90, 18)
                    .build());
        }
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }

    private void finish() {
        config.spawnNotifierTimezone = chosenTimezone;
        config.spawnNotifierSoundEnabled = chosenSound;
        config.notifyUseChat = chosenChat;
        config.onboardingCompleted = true;
        configManager.save();
        Minecraft.getInstance().setScreen(null);
    }

    // -----------------------------------------------------------------
    // Render
    // -----------------------------------------------------------------

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(context);

        int outerLeft = panelLeft - 6;
        int outerTop = panelTop - 6;
        int outerRight = panelLeft + PANEL_WIDTH + 6;
        int outerBottom = panelTop + PANEL_HEIGHT + 6;

        context.fillGradient(outerLeft, outerTop, outerRight, outerBottom, 0xE0181022, 0xE00A0A10);
        context.hLine(outerLeft, outerRight - 1, outerTop, COLOR_TITLE);
        context.hLine(outerLeft, outerRight - 1, outerBottom - 1, COLOR_TITLE);
        context.vLine(outerLeft, outerTop, outerBottom - 1, COLOR_TITLE);
        context.vLine(outerRight - 1, outerTop, outerBottom - 1, COLOR_TITLE);

        int centerX = panelLeft + PANEL_WIDTH / 2;

        context.drawCenteredString(this.font, "GALYX", centerX, panelTop + 8, COLOR_TITLE);
        context.drawCenteredString(this.font, "Paso " + (step + 1) + " / " + TOTAL_STEPS, centerX, panelTop + 20, COLOR_HINT);
        context.hLine(panelLeft, panelLeft + PANEL_WIDTH, panelTop + 30, COLOR_TITLE);

        switch (step) {
            case 0 -> renderStepWelcome(context, centerX);
            case 1 -> {
                context.drawCenteredString(this.font, "¿En que zona horaria juegas?", centerX, panelTop + 36, COLOR_TEXT);
            }
            case 2 -> {
                context.drawCenteredString(this.font, "Avisos antes de que salga un jefe", centerX, panelTop + 36, COLOR_TEXT);
            }
            case 3 -> renderStepDone(context, centerX);
        }

        super.render(context, mouseX, mouseY, partialTick);
    }

    private void renderStepWelcome(GuiGraphics context, int centerX) {
        int y = panelTop + 42;
        context.drawCenteredString(this.font, "¡Bienvenid@ a GALYX!", centerX, y, COLOR_TEXT);
        y += 16;
        context.drawCenteredString(this.font, "Rastrea los jefes de OlympoMC y te", centerX, y, COLOR_HINT);
        y += 10;
        context.drawCenteredString(this.font, "avisa antes de que salgan.", centerX, y, COLOR_HINT);
        y += 16;
        context.drawCenteredString(this.font, "Esto toma menos de 1 minuto.", centerX, y, COLOR_HINT);
    }

    private void renderStepDone(GuiGraphics context, int centerX) {
        int y = panelTop + 40;
        context.drawCenteredString(this.font, "\u00a1Listo!", centerX, y, COLOR_TEXT);
        y += 16;
        context.drawCenteredString(this.font, "Puedes cambiar todo esto despues", centerX, y, COLOR_HINT);
        y += 10;
        context.drawCenteredString(this.font, "escribiendo /galyx en el chat.", centerX, y, COLOR_HINT);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        finish();
    }

}