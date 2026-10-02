package com.galyx.client.trackers;

import com.galyx.client.events.BossDefeatedEvent;
import com.galyx.core.AbstractModule;
import com.galyx.events.EventBus;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Guarda cuantas veces se ha derrotado cada jefe durante la sesion actual.
 * Nunca detecta eventos ni renderiza: solo almacena.
 */
public class BossTracker extends AbstractModule {
    public void reset() {
        bossKills.clear();
    }
    
    private final Map<String, Integer> bossKills = new LinkedHashMap<>();

    public BossTracker(EventBus eventBus) {
        eventBus.subscribe(BossDefeatedEvent.class, this::onBossDefeated);
    }

    @Override
    public String getId() {
        return "boss_tracker";
    }

    @Override
    public String getName() {
        return "Boss Tracker";
    }

    private void onBossDefeated(BossDefeatedEvent event) {
        bossKills.merge(event.getBossName(), 1, Integer::sum);
    }

    public Map<String, Integer> getAllKills() {
        return bossKills;
    }

}