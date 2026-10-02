package com.galyx.client.hud;

public final class HudTheme {

    public static final int[] BORDER_COLORS = {
            0xFFD97BFF, // Morado
            0xFF7CE87C, // Verde
            0xFF66B2FF, // Azul
            0xFFFF6666, // Rojo
            0xFFFFD966, // Dorado
            0xFFFFFFFF, // Blanco
            0xFF5CE8E0, // Cian
            0xFFFF9E4A  // Naranja
    };

    public static final String[] BORDER_COLOR_NAMES = {
            "Morado", "Verde", "Azul", "Rojo", "Dorado", "Blanco", "Cian", "Naranja"
    };

    public static final String[] PRIORITY_NAMES = {
            "Alta", "Media", "Baja"
    };

    public static final String[] ANCHOR_NAMES = {
            "Superior Izquierda", "Superior Derecha", "Inferior Izquierda", "Inferior Derecha"
    };

    public static final String[] SORT_MODE_NAMES = {
            "Prioridad", "Mas derrotas", "Alfabetico"
    };

    public static final int DEFAULT_BACKGROUND = 0x101018;

    /**
     * Color de fondo del panel del HUD. -1 usa el gris/azulado neutro de
     * siempre; 0..7 usa una version oscurecida del color de la paleta para
     * dar un fondo con un tinte del color elegido.
     */
    public static int backgroundAt(int index) {
        if (index < 0) {
            return DEFAULT_BACKGROUND;
        }
        int c = colorAt(index);
        int r = (int) (((c >> 16) & 0xFF) * 0.12);
        int g = (int) (((c >> 8) & 0xFF) * 0.12);
        int b = (int) ((c & 0xFF) * 0.12);
        return (r << 16) | (g << 8) | b;
    }

    public static int colorAt(int index) {
        if (index < 0 || index >= BORDER_COLORS.length) {
            return BORDER_COLORS[0];
        }
        return BORDER_COLORS[index];
    }

    public static String nameAt(int index) {
        if (index < 0 || index >= BORDER_COLOR_NAMES.length) {
            return BORDER_COLOR_NAMES[0];
        }
        return BORDER_COLOR_NAMES[index];
    }

    private HudTheme() {
    }

}