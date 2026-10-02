package com.galyx.client.bridge;

import net.minecraft.network.chat.Component;

import java.util.UUID;
import java.util.function.BiConsumer;

public final class BossBarBridge {

    private static BiConsumer<UUID, Component> listener;

    private BossBarBridge() {
    }

    public static void setListener(BiConsumer<UUID, Component> newListener) {
        listener = newListener;
    }

    public static void fireRemoved(UUID id, Component name) {
        if (listener != null) {
            listener.accept(id, name);
        }
    }

}