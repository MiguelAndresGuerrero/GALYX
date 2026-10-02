package com.galyx.client.trackers;

import com.galyx.client.events.CustomBossDefeatedEvent;
import com.galyx.client.events.CustomBossSurvivedEvent;
import com.galyx.core.AbstractModule;
import com.galyx.events.EventBus;

import java.util.LinkedHashMap;
import java.util.Map;

public class CustomBossTracker extends AbstractModule {

    private final Map<String, Integer> defeated = new LinkedHashMap<>();
    private final Map<String, Integer> survived = new LinkedHashMap<>();

    public CustomBossTracker(EventBus eventBus) {
        eventBus.subscribe(CustomBossDefeatedEvent.class, event ->
                defeated.merge(event.getBossName(), 1, Integer::sum));
        eventBus.subscribe(CustomBossSurvivedEvent.class, event ->
                survived.merge(event.getBossName(), 1, Integer::sum));
    }

    public void reset() {
        defeated.clear();
        survived.clear();
    }

    @Override
    public String getId() {
        return "custom_boss_tracker";
    }

    @Override
    public String getName() {
        return "Custom Boss Tracker";
    }

    public Map<String, Integer> getDefeated() {
        return defeated;
    }

    public Map<String, Integer> getSurvived() {
        return survived;
    }

}