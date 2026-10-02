package com.galyx.client.gui.widget;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;

public class GalyxSlider extends AbstractSliderButton {

    /** Formatea el valor real para mostrarlo en el slider */
    public interface LabelFormatter {
        String format(double actualValue);
    }

    private final double min;
    private final double max;
    private final double step;
    private final LabelFormatter formatter;
    private final DoubleConsumer onChange;
    private double actualValue;

    public GalyxSlider(int x, int y, int width, int height, double min, double max, double step,
                       double initial, LabelFormatter formatter, DoubleConsumer onChange) {
        super(x, y, width, height, Component.literal(""), normalize(snap(initial, min, max, step), min, max));
        this.min = min;
        this.max = max;
        this.step = step;
        this.formatter = formatter;
        this.onChange = onChange;
        this.actualValue = snap(initial, min, max, step);
        updateMessage();
    }

    private static double normalize(double v, double min, double max) {
        if (max <= min) {
            return 0;
        }
        return Math.max(0, Math.min(1, (v - min) / (max - min)));
    }

    private static double snap(double v, double min, double max, double step) {
        double clamped = Math.max(min, Math.min(max, v));
        if (step <= 0) {
            return clamped;
        }
        double snapped = Math.round((clamped - min) / step) * step + min;
        return Math.max(min, Math.min(max, snapped));
    }

    public double getActualValue() {
        return actualValue;
    }

    public void setActualValue(double newValue) {
        this.actualValue = snap(newValue, min, max, step);
        this.value = normalize(this.actualValue, min, max);
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        if (formatter == null) {
            return;
        }
        setMessage(Component.literal(formatter.format(actualValue)));
    }

    @Override
    protected void applyValue() {
        actualValue = snap(min + (max - min) * this.value, min, max, step);
        updateMessage();
        if (onChange != null) {
            onChange.accept(actualValue);
        }
    }

}