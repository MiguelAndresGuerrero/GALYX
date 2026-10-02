package com.galyx.client.bridge;

import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class ActionBarBridge {

    private static Consumer<Component> listener;

    private ActionBarBridge() {
    }

    public static void setListener(Consumer<Component> newListener) {
        listener = newListener;
    }

    public static void fireMessage(Component message) {
        if (listener != null) {
            listener.accept(message);
        }
    }

}