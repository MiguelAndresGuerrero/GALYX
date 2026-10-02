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

public class TabButton extends AbstractWidget {

    private final Font font;
    private final String label;
    private boolean active;
    private int accentColor = 0xFF66B2FF;
    private final Consumer<TabButton> onSelect;

    public TabButton(Font font, int x, int y, int width, int height, String label, boolean active, Consumer<TabButton> onSelect) {
        super(x, y, width, height, Component.literal(label));
        this.font = font;
        this.label = label;
        this.active = active;
        this.onSelect = onSelect;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setAccentColor(int accentColor) {
        this.accentColor = accentColor;
    }

    @Override
    //? if >=1.21.9 {
    /*
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (onSelect != null) {
            onSelect.accept(this);
        }
    }
    */
    //?} else {
    public void onClick(double mouseX, double mouseY) {
        if (onSelect != null) {
            onSelect.accept(this);
        }
    }
    //?}

    @Override
    protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        boolean hovered = isHoveredOrFocused();

        if (active) {
            context.fill(x, y, x + w, y + h, 0x33000000 | (accentColor & 0xFFFFFF));
            context.fill(x, y, x + 3, y + h, accentColor);
        } else if (hovered) {
            context.fill(x, y, x + w, y + h, 0x1EFFFFFF);
        }

        int textColor = active ? accentColor : (hovered ? 0xFFE8E8E8 : 0xFFAAAAAA);
        context.drawString(font, label, x + 12, y + (h - 8) / 2, textColor, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.literal(label));
    }

}