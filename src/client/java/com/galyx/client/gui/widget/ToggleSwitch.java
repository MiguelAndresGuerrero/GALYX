package com.galyx.client.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
//? if >=1.21.9 {
/*import net.minecraft.client.input.MouseButtonEvent;*/
//?}
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Interruptor ON/OFF con etiqueta a la izquierda, usado en toda la pantalla
 * de configuracion en vez de los Checkbox vanilla (se ven mas modernos y
 * coinciden con el estilo del resto del HUD).
 */
public class ToggleSwitch extends AbstractWidget {

    private static final int TRACK_ON = 0xFF57D06B;
    private static final int TRACK_OFF = 0xFF3A3A46;
    private static final int TRACK_BORDER = 0x50000000;
    private static final int KNOB_COLOR = 0xFFF4F4F4;
    private static final int LABEL_COLOR = 0xFFE8E8E8;
    private static final int TRACK_WIDTH = 22;
    private static final int TRACK_HEIGHT = 10;

    private final Font font;
    private boolean value;
    private final Consumer<Boolean> onChange;

    public ToggleSwitch(Font font, int x, int y, int width, String label, boolean initial, Consumer<Boolean> onChange) {
        super(x, y, width, 13, Component.literal(label));
        this.font = font;
        this.value = initial;
        this.onChange = onChange;
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    @Override
    //? if >=1.21.9 {
    /*
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        value = !value;
        if (onChange != null) {
            onChange.accept(value);
        }
    }
    */
    //?} else {
    public void onClick(double mouseX, double mouseY) {
        value = !value;
        if (onChange != null) {
            onChange.accept(value);
        }
    }
    //?}

    @Override
    protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float partialTick) {
        context.drawString(font, getMessage(), getX(), getY() + (getHeight() - 8) / 2, LABEL_COLOR, false);

        int trackX = getX() + getWidth() - TRACK_WIDTH;
        int trackY = getY() + (getHeight() - TRACK_HEIGHT) / 2;
        int trackColor = value ? TRACK_ON : TRACK_OFF;

        context.fill(trackX, trackY, trackX + TRACK_WIDTH, trackY + TRACK_HEIGHT, trackColor);
        context.hLine(trackX, trackX + TRACK_WIDTH - 1, trackY, TRACK_BORDER);
        context.hLine(trackX, trackX + TRACK_WIDTH - 1, trackY + TRACK_HEIGHT - 1, TRACK_BORDER);
        context.vLine(trackX, trackY, trackY + TRACK_HEIGHT - 1, TRACK_BORDER);
        context.vLine(trackX + TRACK_WIDTH - 1, trackY, trackY + TRACK_HEIGHT - 1, TRACK_BORDER);

        int knobSize = TRACK_HEIGHT - 2;
        int knobX = value ? trackX + TRACK_WIDTH - knobSize - 1 : trackX + 1;
        context.fill(knobX, trackY + 1, knobX + knobSize, trackY + 1 + knobSize, KNOB_COLOR);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE,
                Component.literal(getMessage().getString() + ": " + (value ? "activado" : "desactivado")));
    }

}