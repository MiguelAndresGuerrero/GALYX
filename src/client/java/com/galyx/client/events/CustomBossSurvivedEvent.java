package com.galyx.client.events;

import com.galyx.events.Event;

public class CustomBossSurvivedEvent extends Event {

    private final String bossName;

    public CustomBossSurvivedEvent(String bossName) {
        this.bossName = bossName;
    }

    public String getBossName() {
        return bossName;
    }

}