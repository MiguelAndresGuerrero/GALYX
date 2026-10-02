package com.galyx.client.mixin;

import com.galyx.client.bridge.BossBarBridge;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Detecta cuando una barra de jefe desaparece (probable derrota) leyendo
 * el mapa interno de BossHealthOverlay antes y despues de cada paquete.
 * No asumimos que la barra corresponde a una entidad visible: los jefes
 * de plugins de servidor no siempre tienen una entidad de cliente asociada.
 */
@Mixin(BossHealthOverlay.class)
public abstract class BossHealthOverlayMixin {

    @Shadow
    @Final
    private Map<UUID, LerpingBossEvent> events;

    private final Map<UUID, Component> galyx$previousNames = new HashMap<>();

    @Inject(method = "update(Lnet/minecraft/network/protocol/game/ClientboundBossEventPacket;)V", at = @At("TAIL"))
    private void galyx$detectRemovals(ClientboundBossEventPacket packet, CallbackInfo ci) {
        for (Map.Entry<UUID, Component> entry : galyx$previousNames.entrySet()) {
            if (!events.containsKey(entry.getKey())) {
                com.galyx.GALYX.LOGGER.info("[GALYX] Barra de jefe desaparecio: '{}'", entry.getValue().getString());
                BossBarBridge.fireRemoved(entry.getKey(), entry.getValue());
            }
        }
    }

    @Inject(method = "update(Lnet/minecraft/network/protocol/game/ClientboundBossEventPacket;)V", at = @At("HEAD"))
    private void galyx$captureBefore(ClientboundBossEventPacket packet, CallbackInfo ci) {
        for (Map.Entry<UUID, LerpingBossEvent> entry : events.entrySet()) {
            if (!galyx$previousNames.containsKey(entry.getKey())) {
                com.galyx.GALYX.LOGGER.info("[GALYX] Barra de jefe nueva vista: '{}'", entry.getValue().getName().getString());
            }
        }
        galyx$previousNames.clear();
        for (Map.Entry<UUID, LerpingBossEvent> entry : events.entrySet()) {
            galyx$previousNames.put(entry.getKey(), entry.getValue().getName());
        }
    }

}