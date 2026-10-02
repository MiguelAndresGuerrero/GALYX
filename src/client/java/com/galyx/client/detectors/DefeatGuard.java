package com.galyx.client.detectors;

import java.util.HashMap;
import java.util.Map;

public final class DefeatGuard {

    private static final long DEDUPE_WINDOW_MS = 5000;
    private static final Map<String, Long> lastConfirmedAt = new HashMap<>();

    private DefeatGuard() {
    }

    public static boolean tryConfirm(String bossName) {
        long now = System.currentTimeMillis();
        Long last = lastConfirmedAt.get(bossName);
        if (last != null && now - last < DEDUPE_WINDOW_MS) {
            return false;
        }
        lastConfirmedAt.put(bossName, now);
        return true;
    }

    public static void reset() {
        lastConfirmedAt.clear();
    }

}