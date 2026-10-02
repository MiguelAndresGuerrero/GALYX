package com.galyx.client.stats;

import com.galyx.client.trackers.BossTracker;
import com.galyx.client.trackers.CustomBossTracker;

import java.util.Map;

public class StatisticsManager {

    private final BossTracker bossTracker;
    private final CustomBossTracker customBossTracker;
    private final SessionTracker sessionTracker;

    public StatisticsManager(BossTracker bossTracker, CustomBossTracker customBossTracker, SessionTracker sessionTracker) {
        this.bossTracker = bossTracker;
        this.customBossTracker = customBossTracker;
        this.sessionTracker = sessionTracker;
    }

    public Map<String, Integer> getVanillaBossKills() {
        return bossTracker.getAllKills();
    }

    public Map<String, Integer> getCustomBossDefeated() {
        return customBossTracker.getDefeated();
    }

    public Map<String, Integer> getCustomBossSurvived() {
        return customBossTracker.getSurvived();
    }

    public double getElapsedHours() {
        return sessionTracker.getElapsedHours();
    }

}