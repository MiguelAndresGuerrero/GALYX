package com.galyx.client.detectors;

import com.galyx.client.events.BossDefeatedEvent;
import com.galyx.core.AbstractModule;
import com.galyx.events.EventBus;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;

public class BossDetector extends AbstractModule {

    private final EventBus eventBus;

    public BossDetector(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public String getId() {
        return "boss_detector";
    }

    @Override
    public String getName() {
        return "Boss Detector";
    }

    @Override
    public void initialize() {
        ClientEntityEvents.ENTITY_UNLOAD.register(this::onEntityUnload);
    }

    private void onEntityUnload(Entity entity, ClientLevel world) {
        if (!isEnabled()) {
            return;
        }

        String bossName = resolveBossName(entity);
        if (bossName == null) {
            return;
        }

        if (entity instanceof LivingEntity livingEntity && livingEntity.getHealth() <= 0.0f) {
            eventBus.post(new BossDefeatedEvent(livingEntity, bossName));
        }
    }

    private String resolveBossName(Entity entity) {
        if (entity instanceof EnderDragon) {
            return "Ender Dragon";
        }
        if (entity instanceof WitherBoss) {
            return "Wither";
        }
        return null;
    }

}