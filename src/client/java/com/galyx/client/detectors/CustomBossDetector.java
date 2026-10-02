package com.galyx.client.detectors;

import com.galyx.client.bridge.BossBarBridge;
import com.galyx.client.events.CustomBossDefeatedEvent;
import com.galyx.client.events.CustomBossSurvivedEvent;
import com.galyx.core.AbstractModule;
import com.galyx.events.EventBus;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CustomBossDetector extends AbstractModule {

    private static final int CONFIRMATION_WINDOW_TICKS = 60; // ~3 segundos

    private final EventBus eventBus;
    private final Map<String, String> confirmationTextByBoss;
    private final Map<String, Integer> pendingConfirmations = new LinkedHashMap<>();
    private final Map<String, Integer> pendingBaselineCounts = new LinkedHashMap<>();

    public CustomBossDetector(EventBus eventBus, Map<String, String> confirmationTextByBoss) {
        this.eventBus = eventBus;
        this.confirmationTextByBoss = confirmationTextByBoss;
    }

    @Override
    public String getId() {
        return "custom_boss_detector";
    }

    @Override
    public String getName() {
        return "Custom Boss Detector (OlympoMC)";
    }

    @Override
    public void initialize() {
        BossBarBridge.setListener(this::onBossBarRemoved);
        ClientEntityEvents.ENTITY_LOAD.register(this::onEntityLoad);
    }

    @Override
    public void tick() {
        if (pendingConfirmations.isEmpty()) {
            return;
        }

        for (String bossName : List.copyOf(pendingConfirmations.keySet())) {
            int baseline = pendingBaselineCounts.getOrDefault(bossName, 0);
            int current = countMatchingInInventory(bossName);

            if (current > baseline) {
                confirmDefeat(bossName);
            }
        }

        pendingConfirmations.replaceAll((name, ticksLeft) -> ticksLeft - 1);

        pendingConfirmations.entrySet().removeIf(entry -> {
            if (entry.getValue() <= 0) {
                com.galyx.GALYX.LOGGER.info("[GALYX-DEBUG] Se agoto el tiempo sin confirmar: '{}'", entry.getKey());
                pendingBaselineCounts.remove(entry.getKey());
                eventBus.post(new CustomBossSurvivedEvent(entry.getKey()));
                return true;
            }
            return false;
        });
    }

    public void reset() {
        pendingConfirmations.clear();
        pendingBaselineCounts.clear();
    }

    private void onBossBarRemoved(UUID id, Component name) {
        if (!isEnabled()) {
            return;
        }

        com.galyx.GALYX.LOGGER.info("[GALYX-DEBUG] Barra desaparecio, texto: '{}'", name.getString());

        String matched = resolveBossFromBar(name.getString());
        if (matched != null) {
            com.galyx.GALYX.LOGGER.info("[GALYX-DEBUG] Coincide con jefe conocido: '{}'", matched);
            pendingConfirmations.put(matched, CONFIRMATION_WINDOW_TICKS);
            pendingBaselineCounts.put(matched, countMatchingInInventory(matched));
        } else {
            com.galyx.GALYX.LOGGER.info("[GALYX-DEBUG] No coincide con ningun jefe de la lista");
        }
    }

    private void onEntityLoad(Entity entity, ClientLevel level) {
        if (!isEnabled() || pendingConfirmations.isEmpty()) {
            return;
        }

        if (!(entity instanceof ItemEntity itemEntity)) {
            return;
        }

        String itemName = itemEntity.getItem().getHoverName().getString().toUpperCase();

        for (String bossName : List.copyOf(pendingConfirmations.keySet())) {
            String confirmationText = confirmationTextByBoss.get(bossName);
            if (confirmationText != null && itemName.contains(confirmationText.toUpperCase())) {
                confirmDefeat(bossName);
            }
        }
    }

    private void confirmDefeat(String bossName) {
        pendingConfirmations.remove(bossName);
        pendingBaselineCounts.remove(bossName);

        if (!DefeatGuard.tryConfirm(bossName)) {
            return;
        }

        com.galyx.GALYX.LOGGER.info("[GALYX-DEBUG] CONFIRMADO derrotado: '{}'", bossName);
        eventBus.post(new CustomBossDefeatedEvent(bossName));
    }

    private int countMatchingInInventory(String bossName) {
        String confirmationText = confirmationTextByBoss.get(bossName);
        if (confirmationText == null) {
            return 0;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return 0;
        }

        String upperTarget = confirmationText.toUpperCase();
        int count = 0;

        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.getHoverName().getString().toUpperCase().contains(upperTarget)) {
                count += stack.getCount();
            }
        }

        return count;
    }

    private String resolveBossFromBar(String plainText) {
        String upperText = plainText.toUpperCase();
        for (String bossName : confirmationTextByBoss.keySet()) {
            if (upperText.contains(bossName.toUpperCase())) {
                return bossName;
            }
        }
        return null;
    }

}