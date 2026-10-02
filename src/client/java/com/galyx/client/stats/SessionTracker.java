package com.galyx.client.stats;

import com.galyx.core.AbstractModule;

/**
 * Mide cuanto tiempo llevas en la sesion actual (mundo/servidor), para
 * poder calcular jefes derrotados por hora. Se reinicia cada vez que
 * entras a un mundo o servidor nuevo, igual que los demas trackers.
 */
public class SessionTracker extends AbstractModule {

    private long sessionStartMillis;

    @Override
    public String getId() {
        return "session_tracker";
    }

    @Override
    public String getName() {
        return "Session Tracker";
    }

    @Override
    public void initialize() {
        sessionStartMillis = System.currentTimeMillis();
    }

    public void reset() {
        sessionStartMillis = System.currentTimeMillis();
    }

    public double getElapsedHours() {
        long elapsedMs = System.currentTimeMillis() - sessionStartMillis;
        return Math.max(elapsedMs / 3_600_000.0, 1.0 / 60.0);
    }

}