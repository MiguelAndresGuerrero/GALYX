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

public class SwatchButton extends AbstractWidget {

    private final int swatchColor;
    private boolean selected;
    private final Consumer<SwatchButton> onSelect;
    private Font font;
    private String glyph;

    public SwatchButton(int x, int y, int size, int swatchColor, String name, boolean selected, Consumer<SwatchButton> onSelect) {
        super(x, y, size, size, Component.literal(name));
        this.swatchColor = swatchColor;
        this.selected = selected;
        this.onSelect = onSelect;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setGlyph(Font font, String glyph) {
        this.font = font;
        this.glyph = glyph;
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

        context.fill(x, y, x + w, y + h, 0xFF000000 | (swatchColor & 0xFFFFFF));

        int borderColor = selected ? 0xFFFFFFFF : 0x60FFFFFF;
        context.hLine(x, x + w - 1, y, borderColor);
        context.hLine(x, x + w - 1, y + h - 1, borderColor);
        context.vLine(x, y, y + h - 1, borderColor);
        context.vLine(x + w - 1, y, y + h - 1, borderColor);

        if (glyph != null && font != null) {
            int textWidth = font.width(glyph);
            context.drawString(font, glyph, x + (w - textWidth) / 2, y + (h - 8) / 2, 0xFFFFFFFF, true);
        } else if (selected) {
            context.fill(x + w / 2 - 1, y + h / 2 - 1, x + w / 2 + 1, y + h / 2 + 1, 0xFF101018);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }

}