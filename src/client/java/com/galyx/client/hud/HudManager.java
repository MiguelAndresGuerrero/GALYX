package com.galyx.client.hud;

import com.galyx.GALYX;
import com.galyx.client.config.GalyxConfig;
import com.galyx.client.notify.BossSpawnNotifier;
import com.galyx.client.notify.SpawnTimeUtils;
import com.galyx.client.stats.StatisticsManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HudManager {

    private static final String ZAFIRO_DROP_NAME = "FRAGMENTO DE ZAFIRO";
    private static final int PRIORITY_BAJA = 2; // indice en HudTheme.PRIORITY_NAMES

    private static final int COLOR_TEXT = 0xFFE8E8E8;
    private static final int COLOR_NAME = 0xFFFFD966;
    private static final int COLOR_TOTAL = 0xFFFFFFFF;
    private static final int COLOR_EXTRA = 0xFFC9C9D6;

    private static final int LINE_HEIGHT = 14;
    private static final int PADDING = 6;
    private static final int CORNER_SIZE = 5;

    private static final Map<String, ResourceLocation> BOSS_ICONS = new LinkedHashMap<>();

    static {
        BOSS_ICONS.put("REY NEPTUNO", icon("rey_neptuno"));
        BOSS_ICONS.put("CLEOPATRA", icon("cleopatra"));
        BOSS_ICONS.put("REY GOBLIN", icon("rey_goblin"));
        BOSS_ICONS.put("REY PIGLIN", icon("rey_piglin"));
        BOSS_ICONS.put("AZAZEL", icon("azazel"));
        BOSS_ICONS.put("PALADÍN ESQUELETOR", icon("paladin_esqueletor"));
    }

    private static ResourceLocation icon(String fileName) {
        return ResourceLocation.fromNamespaceAndPath(GALYX.MOD_ID, "textures/gui/icon_" + fileName + ".png");
    }

    private final StatisticsManager statisticsManager;
    private final GalyxConfig config;

    // Cache de los textos "pesados" de recalcular (proximo spawn, tiempo
    // jugado, coordenadas). Se refrescan cada hudUpdateIntervalTicks ticks
    // aprox (1 tick =~ 50 ms) en vez de en cada frame.
    private boolean cacheWarm = false;
    private long lastComputeMillis = 0L;
    private String cachedCountdownLine;
    private String cachedPlaytimeLine;
    private String cachedCoordsLine;

    public HudManager(StatisticsManager statisticsManager, GalyxConfig config) {
        this.statisticsManager = statisticsManager;
        this.config = config;
    }

    public void render(GuiGraphics context, DeltaTracker tickCounter) {
        if (!config.hudEnabled) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        int iconSize = config.hudIconSize;
        int primaryColor = HudTheme.colorAt(config.hudBorderColorIndex);
        int secondaryColor = HudTheme.colorAt(config.hudSecondaryColorIndex);
        int separatorColor = (0x80 << 24) | (primaryColor & 0xFFFFFF);
        int borderColor = (0x60 << 24) | (primaryColor & 0xFFFFFF);
        int panelBgColor = (config.hudPanelAlpha << 24) | HudTheme.backgroundAt(config.hudBackgroundColorIndex);
        boolean shadow = config.hudTextShadow;

        List<BossLine> bossLines = buildBossLines();
        int totalDefeated = bossLines.stream().mapToInt(l -> l.count).sum();
        String totalLine = "Total: " + totalDefeated + " jefes derrotados";

        refreshCachedLines();

        // La linea de "Proximo Fragmento" se trata aparte del resto de
        // estadisticas: usa el color secundario y va justo despues de
        // "Total", igual que en la vista previa de la configuracion.
        String countdownLine = (config.hudShowTotal && cachedCountdownLine != null) ? cachedCountdownLine : null;

        List<String> extraLines = new ArrayList<>();
        if (config.hudShowPlaytime && cachedPlaytimeLine != null) {
            extraLines.add(cachedPlaytimeLine);
        }
        if (config.hudShowFragments) {
            extraLines.add("Fragmentos: " + countFragments());
        }
        if (config.hudShowFps) {
            extraLines.add("FPS: " + Minecraft.getInstance().getFps());
        }
        if (config.hudShowPing) {
            extraLines.add("Ping: " + resolvePing() + " ms");
        }
        if (config.hudShowCoords && cachedCoordsLine != null) {
            extraLines.add(cachedCoordsLine);
        }

        context.pose().pushMatrix();
        context.pose().scale(config.hudScale, config.hudScale);

        int maxWidth = font.width("GALYX");
        maxWidth = Math.max(maxWidth, font.width(totalLine));
        if (countdownLine != null) {
            maxWidth = Math.max(maxWidth, font.width(countdownLine));
        }
        for (String line : extraLines) {
            maxWidth = Math.max(maxWidth, font.width(line));
        }
        for (BossLine line : bossLines) {
            maxWidth = Math.max(maxWidth, lineWidth(font, iconSize, line.name, line.count));
        }

        int bossBlockHeight;
        if (bossLines.isEmpty()) {
            bossBlockHeight = LINE_HEIGHT;
        } else {
            bossBlockHeight = 0;
            for (BossLine line : bossLines) {
                bossBlockHeight += rowHeight(font, iconSize, line);
            }
        }
        int countdownHeight = countdownLine == null ? 0 : (4 + LINE_HEIGHT);
        int extrasHeight = extraLines.isEmpty() ? 0 : (4 + extraLines.size() * LINE_HEIGHT);
        // titulo + separador, bloque de jefes, separador + total, [separador + proximo fragmento], separador + extras
        int contentHeight = (LINE_HEIGHT - 4) + 4 + bossBlockHeight + 4 + LINE_HEIGHT + countdownHeight + extrasHeight;

        int panelWidth = maxWidth + PADDING * 2;
        int panelHeight = contentHeight + PADDING * 2;

        // El HUD se puede anclar a cualquiera de las 4 esquinas de la
        // pantalla; hudX/hudY se miden como distancia desde esa esquina
        // (en vez de siempre desde la esquina superior izquierda).
        float scale = config.hudScale <= 0f ? 1f : config.hudScale;
        int screenW = (int) (Minecraft.getInstance().getWindow().getGuiScaledWidth() / scale);
        int screenH = (int) (Minecraft.getInstance().getWindow().getGuiScaledHeight() / scale);

        int panelLeft;
        int panelTop;
        switch (config.hudAnchor) {
            case 1 -> { // Superior Derecha
                panelLeft = screenW - config.hudX - panelWidth;
                panelTop = config.hudY;
            }
            case 2 -> { // Inferior Izquierda
                panelLeft = config.hudX;
                panelTop = screenH - config.hudY - panelHeight;
            }
            case 3 -> { // Inferior Derecha
                panelLeft = screenW - config.hudX - panelWidth;
                panelTop = screenH - config.hudY - panelHeight;
            }
            default -> { // Superior Izquierda
                panelLeft = config.hudX;
                panelTop = config.hudY;
            }
        }

        int x = panelLeft + PADDING;
        int y = panelTop + PADDING;
        int panelRight = panelLeft + panelWidth;
        int panelBottom = panelTop + panelHeight;

        context.fill(panelLeft, panelTop, panelRight, panelBottom, panelBgColor);

        if (config.hudShowBorder) {
            context.hLine(panelLeft, panelRight - 1, panelTop, borderColor);
            context.hLine(panelLeft, panelRight - 1, panelBottom - 1, borderColor);
            context.vLine(panelLeft, panelTop, panelBottom - 1, borderColor);
            context.vLine(panelRight - 1, panelTop, panelBottom - 1, borderColor);
        }

        if (config.hudRoundedCorners) {
            drawCorner(context, panelLeft, panelTop, 1, 1, primaryColor);
            drawCorner(context, panelRight, panelTop, -1, 1, primaryColor);
            drawCorner(context, panelLeft, panelBottom, 1, -1, primaryColor);
            drawCorner(context, panelRight, panelBottom, -1, -1, primaryColor);
        }

        context.drawString(font, "GALYX", x, y, primaryColor, shadow);
        y += LINE_HEIGHT - 4;

        context.hLine(x, x + maxWidth, y, separatorColor);
        y += 4;

        if (bossLines.isEmpty()) {
            context.drawString(font, "Sin jefes derrotados aun", x, y, COLOR_TEXT, shadow);
            y += LINE_HEIGHT;
        } else {
            for (BossLine line : bossLines) {
                y = drawBossLine(context, font, iconSize, x, y, line, secondaryColor, shadow);
            }
        }

        context.hLine(x, x + maxWidth, y, separatorColor);
        y += 4;
        context.drawString(font, totalLine, x, y, COLOR_TOTAL, shadow);
        y += LINE_HEIGHT;

        if (countdownLine != null) {
            context.hLine(x, x + maxWidth, y, separatorColor);
            y += 4;
            context.drawString(font, countdownLine, x, y, secondaryColor, shadow);
            y += LINE_HEIGHT;
        }

        // Un solo separador antes de todo el bloque de estadisticas extra
        // (no uno por cada linea, para que no se vea "a rayas").
        if (!extraLines.isEmpty()) {
            context.hLine(x, x + maxWidth, y, separatorColor);
            y += 4;
            for (String line : extraLines) {
                context.drawString(font, line, x, y, COLOR_EXTRA, shadow);
                y += LINE_HEIGHT;
            }
        }

        context.pose().popMatrix();
    }

    private void drawCorner(GuiGraphics context, int x, int y, int dirX, int dirY, int color) {
        context.hLine(x, x + dirX * CORNER_SIZE, y, color);
        context.vLine(x, y, y + dirY * CORNER_SIZE, color);
    }

    private int lineWidth(Font font, int iconSize, String bossName, int count) {
        if (config.hudUseIcons && BOSS_ICONS.containsKey(bossName)) {
            return iconSize + 4 + font.width(String.valueOf(count));
        }
        return font.width(bossName + ": " + count);
    }

    /**
     * Alto que ocupa la fila de este jefe. Normalmente es LINE_HEIGHT, pero
     * si el jefe tiene icono y el usuario puso un "Tamaño de icono" grande
     * (hasta 32px, mientras LINE_HEIGHT es fijo en 14), la fila crece para
     * que el icono no se superponga con el texto de la fila siguiente.
     */
    private int rowHeight(Font font, int iconSize, BossLine line) {
        boolean hasIcon = config.hudUseIcons && BOSS_ICONS.containsKey(line.name);
        return hasIcon ? Math.max(LINE_HEIGHT, iconSize + 4) : LINE_HEIGHT;
    }

    private int drawBossLine(GuiGraphics context, Font font, int iconSize, int x, int y, BossLine line, int secondaryColor, boolean shadow) {
        ResourceLocation icon = config.hudUseIcons ? BOSS_ICONS.get(line.name) : null;
        int valueColor = line.colorIndex >= 0 ? HudTheme.colorAt(line.colorIndex) : secondaryColor;
        // El nombre tambien toma el color propio del jefe (si se eligio uno
        // en la pestaña "Jefes"); si esta en "auto" (-1) usa el dorado de
        // siempre. Antes solo el numero cambiaba de color y el cambio
        // casi no se notaba.
        int nameColor = line.colorIndex >= 0 ? HudTheme.colorAt(line.colorIndex) : COLOR_NAME;

        int rowH = rowHeight(font, iconSize, line);

        if (icon != null) {
            // Icono alineado arriba de la fila; el numero se centra
            // verticalmente respecto al icono (antes quedaba pegado arriba
            // cuando el icono era mas grande que una linea de texto).
            int iconY = y - 2;
            int textY = iconY + (iconSize - 8) / 2;
            context.blit(
                    RenderPipelines.GUI_TEXTURED, icon,
                    x, iconY, 0, 0,
                    iconSize, iconSize, iconSize, iconSize
            );
            context.drawString(font, String.valueOf(line.count), x + iconSize + 4, textY, valueColor, shadow);
        } else {
            context.drawString(font, line.name + ": ", x, y, nameColor, shadow);
            int nameWidth = font.width(line.name + ": ");
            context.drawString(font, String.valueOf(line.count), x + nameWidth, y, valueColor, shadow);
        }

        return y + rowH;
    }

    /**
     * Junta los jefes vanilla + OlympoMC derrotados, descarta los que el
     * usuario desactivo en la pestaña "Jefes", los ordena segun
     * hudSortMode y recorta la lista a hudMaxBossesShown.
     */
    private List<BossLine> buildBossLines() {
        Map<String, Integer> totals = new LinkedHashMap<>();
        totals.putAll(statisticsManager.getVanillaBossKills());

        Map<String, Integer> defeated = statisticsManager.getCustomBossDefeated();
        for (Map.Entry<String, Integer> entry : defeated.entrySet()) {
            totals.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }

        List<BossLine> lines = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : totals.entrySet()) {
            String name = entry.getKey();
            if (!config.isBossEnabled(name)) {
                continue;
            }
            lines.add(new BossLine(name, entry.getValue(), config.getBossPriority(name), config.getBossColorIndex(name)));
        }

        switch (config.hudSortMode) {
            case 1 -> lines.sort(Comparator.<BossLine>comparingInt(l -> l.count).reversed().thenComparing(l -> l.name));
            case 2 -> lines.sort(Comparator.comparing(l -> l.name));
            default -> lines.sort(Comparator.<BossLine>comparingInt(l -> l.priority).thenComparing(l -> l.name));
        }

        int max = Math.max(1, config.hudMaxBossesShown);
        if (lines.size() > max) {
            lines = new ArrayList<>(lines.subList(0, max));
        }
        return lines;
    }

    private int countFragments() {
        int total = 0;
        Map<String, Integer> defeated = statisticsManager.getCustomBossDefeated();
        for (Map.Entry<String, String> entry : config.customBosses.entrySet()) {
            if (!ZAFIRO_DROP_NAME.equalsIgnoreCase(entry.getValue())) {
                continue;
            }
            Integer count = defeated.get(entry.getKey());
            if (count != null) {
                total += count;
            }
        }
        return total;
    }

    private int resolvePing() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return 0;
        }
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return 0;
        }
        PlayerInfo info = connection.getPlayerInfo(player.getUUID());
        return info != null ? info.getLatency() : 0;
    }

    private void refreshCachedLines() {
        long intervalMillis = Math.max(1, config.hudUpdateIntervalTicks) * 50L;
        long now = System.currentTimeMillis();
        if (cacheWarm && now - lastComputeMillis < intervalMillis) {
            return;
        }
        lastComputeMillis = now;
        cacheWarm = true;

        cachedCountdownLine = config.hudShowTotal ? buildNextZafiroLine() : null;
        cachedPlaytimeLine = config.hudShowPlaytime ? buildPlaytimeLine() : null;
        cachedCoordsLine = config.hudShowCoords ? buildCoordsLine() : null;
    }

    private String buildPlaytimeLine() {
        double hours = statisticsManager.getElapsedHours();
        int wholeHours = (int) hours;
        int minutes = (int) Math.round((hours - wholeHours) * 60);
        if (wholeHours > 0) {
            return "Tiempo jugado: " + wholeHours + "h " + minutes + "m";
        }
        return "Tiempo jugado: " + minutes + "m";
    }

    private String buildCoordsLine() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return "XYZ: -";
        }
        return "XYZ: " + (int) player.getX() + " / " + (int) player.getY() + " / " + (int) player.getZ();
    }

    private String buildNextZafiroLine() {
        ZonedDateTime now = ZonedDateTime.now(BossSpawnNotifier.resolveZone(config.spawnNotifierTimezone));

        String bestBoss = null;
        ZonedDateTime bestTime = null;

        // Se recorren TODOS los jefes con horario programado (no solo los
        // que sueltan Fragmento de Zafiro), para que esta linea siempre
        // coincida con el jefe mas cercano de verdad -- el mismo que
        // dispara el aviso de spawn. Sigue respetando prioridad y
        // habilitado, igual que antes.
        for (Map.Entry<String, List<String>> entry : config.bossSpawnTimes.entrySet()) {
            String bossName = entry.getKey();

            // Solo se consideran jefes con prioridad Alta o Media (se
            // descartan los de prioridad Baja) y que esten habilitados en
            // la pestaña "Jefes". Entre los que quedan, se elige el que
            // tenga el spawn mas cercano en el tiempo.
            if (config.getBossPriority(bossName) >= PRIORITY_BAJA) {
                continue;
            }
            if (!config.isBossEnabled(bossName)) {
                continue;
            }

            List<String> times = entry.getValue();
            if (times == null) {
                continue;
            }

            for (String timeText : times) {
                LocalTime spawnTime = SpawnTimeUtils.parseTime(timeText);
                if (spawnTime == null) {
                    continue;
                }

                ZonedDateTime occurrence = SpawnTimeUtils.nextOccurrence(spawnTime, now);
                if (bestTime == null || occurrence.isBefore(bestTime)) {
                    bestTime = occurrence;
                    bestBoss = bossName;
                }
            }
        }

        if (bestBoss == null) {
            return "Proximo jefe: sin horario configurado";
        }

        Duration remaining = Duration.between(now, bestTime);
        long totalMinutes = remaining.toMinutes();
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        String timeText;
        if (hours > 0) {
            timeText = hours + "h " + minutes + "m";
        } else if (minutes > 0) {
            timeText = minutes + "m";
        } else {
            timeText = remaining.getSeconds() + "s";
        }

        return "Proximo jefe: " + bestBoss + " en " + timeText;
    }

    private static final class BossLine {
        final String name;
        final int count;
        final int priority;
        final int colorIndex;

        BossLine(String name, int count, int priority, int colorIndex) {
            this.name = name;
            this.count = count;
            this.priority = priority;
            this.colorIndex = colorIndex;
        }
    }

}