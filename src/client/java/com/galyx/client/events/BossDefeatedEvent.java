package com.galyx.client.events;

import com.galyx.events.Event;
import net.minecraft.world.entity.LivingEntity;

public class BossDefeatedEvent extends Event {

    private final LivingEntity boss;
    private final String bossName;

    public BossDefeatedEvent(LivingEntity boss, String bossName) {
        this.boss = boss;
        this.bossName = bossName;
    }

    public LivingEntity getBoss() {
        return boss;
    }

    public String getBossName() {
        return bossName;
    }

}