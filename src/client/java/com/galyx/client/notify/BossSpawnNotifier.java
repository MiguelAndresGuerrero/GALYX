package com.galyx.client.notify;

import com.galyx.client.config.GalyxConfig;
import com.galyx.core.AbstractModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BossSpawnNotifier extends AbstractModule {

    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final ZoneId FALLBACK_ZONE = ZoneId.of("America/Lima");

    private final GalyxConfig config;
    private final Set<String> alreadyNotified = new HashSet<>();
    private int tickCounter;

    public BossSpawnNotifier(GalyxConfig config) {
        this.config = config;
    }

    @Override
    public String getId() {
        return "boss_spawn_notifier";
    }

    @Override
    public String getName() {
        return "Boss Spawn Notifier";
    }

    @Override
    public void tick() {
        if (!isEnabled()) {
            return;
        }

        tickCounter++;
        if (tickCounter < CHECK_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        ZonedDateTime now = ZonedDateTime.now(resolveZone(config.spawnNotifierTimezone));

        for (Map.Entry<String, List<String>> entry : config.bossSpawnTimes.entrySet()) {
            String bossName = entry.getKey();
            for (String timeText : entry.getValue()) {
                checkSpawnTime(bossName, timeText, now);
            }
        }
    }

    public void reset() {
        alreadyNotified.clear();
    }

    public static ZoneId resolveZone(String configuredZone) {
        try {
            return ZoneId.of(configuredZone.trim());
        } catch (Exception e) {
            return FALLBACK_ZONE;
        }
    }

    private void checkSpawnTime(String bossName, String timeText, ZonedDateTime now) {
        LocalTime spawnTime = SpawnTimeUtils.parseTime(timeText);
        if (spawnTime == null) {
            return;
        }

        int warningMinutes = Math.max(1, config.notifyWarningMinutes);

        ZonedDateTime nextOccurrence = SpawnTimeUtils.nextOccurrence(spawnTime, now);
        long secondsUntil = Duration.between(now, nextOccurrence).getSeconds();
        String key = bossName + "|" + timeText + "|" + nextOccurrence.toLocalDate();

        if (secondsUntil <= warningMinutes * 60L && secondsUntil > 0 && !alreadyNotified.contains(key)) {
            alreadyNotified.add(key);
            sendWarning(bossName, warningMinutes);
        }
    }

    private void sendWarning(String bossName, int warningMinutes) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        String minuteWord = warningMinutes == 1 ? "minuto" : "minutos";
        Component message = Component.literal("GALYX: " + bossName + " sale en " + warningMinutes + " " + minuteWord);

        player.displayClientMessage(message, !config.notifyUseChat);

        if (config.spawnNotifierSoundEnabled) {
            Minecraft.getInstance().getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F
                    )
            );
        }
    }

}