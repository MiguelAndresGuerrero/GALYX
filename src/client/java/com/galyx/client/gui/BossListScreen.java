package com.galyx.client.gui;

import com.galyx.client.config.ConfigManager;
import com.galyx.client.config.GalyxConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BossListScreen extends Screen {

    private static final int COLOR_TITLE = 0xFFD97BFF;
    private static final int COLOR_SUBTITLE = 0xFFAAAAAA;
    private static final int COLOR_LABEL = 0xFFE8E8E8;
    private static final int COLOR_PANEL_BG = 0xC0101018;
    private static final int COLOR_PANEL_BORDER = 0xFFD97BFF;

    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_MARGIN = 10;
    private static final int ROWS_PER_PAGE = 5;
    private static final int ROW_HEIGHT = 26;

    private final GalyxConfig config;
    private final ConfigManager configManager;
    private final List<String[]> entries;
    private final int page;

    private int panelLeft;
    private int panelTop;
    private int panelHeight;
    private int listTop;

    public BossListScreen(GalyxConfig config, ConfigManager configManager) {
        this(config, configManager, copyEntries(config), 0);
    }

    private BossListScreen(GalyxConfig config, ConfigManager configManager, List<String[]> entries, int page) {
        super(Component.literal("GALYX - Jefes"));
        this.config = config;
        this.configManager = configManager;
        this.entries = entries;
        this.page = page;
    }

    private static List<String[]> copyEntries(GalyxConfig config) {
        List<String[]> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : config.customBosses.entrySet()) {
            list.add(new String[]{entry.getKey(), entry.getValue()});
        }
        return list;
    }

    private int totalPages() {
        return Math.max(1, (int) Math.ceil(entries.size() / (double) ROWS_PER_PAGE));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        panelLeft = centerX - PANEL_WIDTH / 2;
        panelTop = this.height / 2 - 130;

        listTop = panelTop + 36;

        int startIndex = page * ROWS_PER_PAGE;
        int endIndex = Math.min(startIndex + ROWS_PER_PAGE, entries.size());
        int y = listTop;

        for (int i = startIndex; i < endIndex; i++) {
            int rowIndex = i;
            String[] entry = entries.get(i);

            EditBox nameBox = new EditBox(this.font, panelLeft, y, 130, 20, Component.literal("Nombre"));
            nameBox.setMaxLength(64);
            nameBox.setValue(entry[0]);
            nameBox.setResponder(text -> entry[0] = text);
            addRenderableWidget(nameBox);

            EditBox confirmBox = new EditBox(this.font, panelLeft + 138, y, 130, 20, Component.literal("Confirmacion"));
            confirmBox.setMaxLength(64);
            confirmBox.setValue(entry[1]);
            confirmBox.setResponder(text -> entry[1] = text);
            addRenderableWidget(confirmBox);

            addRenderableWidget(Button.builder(Component.literal("X"), button -> removeEntry(rowIndex))
                    .bounds(panelLeft + 276, y, 20, 20)
                    .build());

            y += ROW_HEIGHT;
        }

        int bottomY = listTop + ROWS_PER_PAGE * ROW_HEIGHT + 6;

        addRenderableWidget(Button.builder(Component.literal("< Anterior"), button -> changePage(-1))
                .bounds(panelLeft, bottomY, 90, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Siguiente >"), button -> changePage(1))
                .bounds(panelLeft + 206, bottomY, 90, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("+ Agregar jefe"), button -> addEntry())
                .bounds(panelLeft, bottomY + 26, 296, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Guardar y volver"), button -> saveAndReturn())
                .bounds(panelLeft, bottomY + 52, 145, 20)
                .build());

        addRenderableWidget(Button.builder(
                        Component.literal("Cancelar"),
                        button -> Minecraft.getInstance().setScreen(new ConfigScreen(config, configManager, null, ConfigScreen.Tab.JEFES)))
                .bounds(panelLeft + 151, bottomY + 52, 145, 20)
                .build());

        panelHeight = (bottomY + 52 + 20 + 16) - panelTop;
    }

    private void changePage(int delta) {
        int newPage = page + delta;
        if (newPage < 0 || newPage >= totalPages()) {
            return;
        }
        Minecraft.getInstance().setScreen(new BossListScreen(config, configManager, entries, newPage));
    }

    private void addEntry() {
        entries.add(new String[]{"", ""});
        int newPage = totalPages() - 1;
        Minecraft.getInstance().setScreen(new BossListScreen(config, configManager, entries, newPage));
    }

    private void removeEntry(int index) {
        entries.remove(index);
        int newPage = Math.min(page, totalPages() - 1);
        Minecraft.getInstance().setScreen(new BossListScreen(config, configManager, entries, newPage));
    }

    private void saveAndReturn() {
        Map<String, String> rebuilt = new LinkedHashMap<>();
        for (String[] entry : entries) {
            String name = entry[0].trim();
            String confirmText = entry[1].trim();
            if (!name.isEmpty() && !confirmText.isEmpty()) {
                rebuilt.put(name, confirmText);
            }
        }
        config.customBosses.clear();
        config.customBosses.putAll(rebuilt);
        configManager.save();
        Minecraft.getInstance().setScreen(new ConfigScreen(config, configManager, null, ConfigScreen.Tab.JEFES));
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(context);

        int centerX = this.width / 2;

        context.fill(
                panelLeft - PANEL_MARGIN, panelTop - PANEL_MARGIN,
                panelLeft + PANEL_WIDTH + PANEL_MARGIN, panelTop + panelHeight,
                COLOR_PANEL_BG
        );
        context.hLine(panelLeft - PANEL_MARGIN, panelLeft + PANEL_WIDTH + PANEL_MARGIN, panelTop - PANEL_MARGIN, COLOR_PANEL_BORDER);
        context.hLine(panelLeft - PANEL_MARGIN, panelLeft + PANEL_WIDTH + PANEL_MARGIN, panelTop + panelHeight - 1, COLOR_PANEL_BORDER);
        context.vLine(panelLeft - PANEL_MARGIN, panelTop - PANEL_MARGIN, panelTop + panelHeight, COLOR_PANEL_BORDER);
        context.vLine(panelLeft + PANEL_WIDTH + PANEL_MARGIN, panelTop - PANEL_MARGIN, panelTop + panelHeight, COLOR_PANEL_BORDER);

        context.drawCenteredString(this.font, "GALYX", centerX, panelTop - PANEL_MARGIN + 6, COLOR_TITLE);
        context.drawCenteredString(
                this.font,
                "Jefes (" + entries.size() + ") - Pagina " + (page + 1) + "/" + totalPages(),
                centerX, panelTop - PANEL_MARGIN + 18, COLOR_SUBTITLE
        );

        if (!entries.isEmpty()) {
            context.drawString(this.font, "Nombre", panelLeft, listTop - 10, COLOR_LABEL, false);
            context.drawString(this.font, "Confirmacion", panelLeft + 138, listTop - 10, COLOR_LABEL, false);
        } else {
            context.drawCenteredString(this.font, "Sin jefes registrados. Usa \"+ Agregar jefe\".", centerX, listTop + 10, COLOR_LABEL);
        }

        super.render(context, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

}