package com.galyx.client.events;

import com.galyx.events.Event;

public class CustomBossDefeatedEvent extends Event {

    private final String bossName;

    public CustomBossDefeatedEvent(String bossName) {
        this.bossName = bossName;
    }

    public String getBossName() {
        return bossName;
    }

}