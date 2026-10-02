package com.galyx.client.detectors;

import com.galyx.client.bridge.ActionBarBridge;
import com.galyx.client.events.CustomBossDefeatedEvent;
import com.galyx.core.AbstractModule;
import com.galyx.events.EventBus;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActionBarBossDetector extends AbstractModule {

    private static final Pattern PATTERN = Pattern.compile(
            "\\|\\s*(.+?)\\s*-\\s*\\[\\s*(\\d+)\\s*/\\s*(\\d+)\\s*]"
    );

    // Se considera "cerca de morir" cuando la vida cae por debajo de este
    // porcentaje del maximo (ej: 5% de 2000 = 100).
    private static final double LOW_HP_PERCENT = 0.05;
    // Una lectura de vida baja deja de contar como candidato a confirmar
    // por loot despues de este tiempo, para no confirmar por error un
    // combate viejo abandonado.
    private static final long NEAR_DEATH_EXPIRY_MS = 5 * 60 * 1000L;

    private final EventBus eventBus;
    private final Map<String, String> confirmationTextByBoss;
    private final Map<String, Boolean> alreadyDeadFlag = new HashMap<>();
    private final Map<String, Long> nearDeathAt = new HashMap<>();

    /**
     * @param confirmationTextByBoss el mismo Map que usa CustomBossDetector
     *                               (config.customBosses): nombre del jefe
     *                               tal como aparece en el action bar ->
     *                               texto del item que confirma su muerte.
     */
    public ActionBarBossDetector(EventBus eventBus, Map<String, String> confirmationTextByBoss) {
        this.eventBus = eventBus;
        this.confirmationTextByBoss = confirmationTextByBoss;
    }

    @Override
    public String getId() {
        return "action_bar_boss_detector";
    }

    @Override
    public String getName() {
        return "Action Bar Boss Detector";
    }

    @Override
    public void initialize() {
        ActionBarBridge.setListener(this::onActionBarMessage);
        ClientEntityEvents.ENTITY_UNLOAD.register(this::onEntityUnload);
        ClientEntityEvents.ENTITY_LOAD.register(this::onEntityLoad);
    }

    public void reset() {
        alreadyDeadFlag.clear();
        nearDeathAt.clear();
    }

    private void onActionBarMessage(Component message) {
        if (!isEnabled()) {
            return;
        }

        String text = message.getString();
        Matcher matcher = PATTERN.matcher(text);
        if (!matcher.find()) {
            return;
        }

        String rawName = matcher.group(1).trim();
        int currentHp;
        int maxHp;
        try {
            currentHp = Integer.parseInt(matcher.group(2));
            maxHp = Integer.parseInt(matcher.group(3));
        } catch (NumberFormatException e) {
            return;
        }

        String matchedBoss = resolveKnownBoss(rawName);
        if (matchedBoss == null) {
            return;
        }

        com.galyx.GALYX.LOGGER.info("[GALYX-ACTIONBAR] Detectado '{}' con {} de vida (texto: '{}')", matchedBoss, currentHp, text);

        boolean wasDead = alreadyDeadFlag.getOrDefault(matchedBoss, false);

        if (currentHp <= 0 && !wasDead) {
            alreadyDeadFlag.put(matchedBoss, true);
            nearDeathAt.remove(matchedBoss);
            confirmDefeat(matchedBoss, "action bar en 0");
            return;
        }

        if (currentHp > 0) {
            alreadyDeadFlag.put(matchedBoss, false);
        }

        if (maxHp > 0 && currentHp > 0 && currentHp <= Math.max(1, Math.round(maxHp * LOW_HP_PERCENT))) {
            nearDeathAt.put(matchedBoss, System.currentTimeMillis());
        } else if (currentHp > 0) {
            // La vida volvio a subir (jefe nuevo, regen, etc.): ya no es
            // candidato a confirmarse por el loot de un combate anterior.
            nearDeathAt.remove(matchedBoss);
        }
    }

    private void onEntityUnload(Entity entity, ClientLevel level) {
        if (!isEnabled()) {
            return;
        }

        if (!(entity instanceof LivingEntity livingEntity)) {
            return;
        }

        Component customName = livingEntity.getCustomName();
        if (customName == null) {
            return;
        }

        String matchedBoss = resolveKnownBoss(customName.getString());
        if (matchedBoss == null) {
            return;
        }

        if (livingEntity.getHealth() <= 0.0f) {
            com.galyx.GALYX.LOGGER.info("[GALYX-ACTIONBAR] Entidad '{}' desaparecio con salud 0", matchedBoss);
            nearDeathAt.remove(matchedBoss);
            confirmDefeat(matchedBoss, "entidad removida con salud 0");
        }
    }

    private void onEntityLoad(Entity entity, ClientLevel level) {
        if (!isEnabled() || nearDeathAt.isEmpty()) {
            return;
        }

        if (!(entity instanceof ItemEntity itemEntity)) {
            return;
        }

        String itemName = itemEntity.getItem().getHoverName().getString().toUpperCase();
        long now = System.currentTimeMillis();

        String bestCandidate = null;
        long bestTimestamp = -1;

        for (Map.Entry<String, Long> entry : nearDeathAt.entrySet()) {
            String bossName = entry.getKey();
            long seenAt = entry.getValue();

            if (now - seenAt > NEAR_DEATH_EXPIRY_MS) {
                continue;
            }

            String confirmationText = confirmationTextByBoss.get(bossName);
            if (confirmationText == null || !itemName.contains(confirmationText.toUpperCase())) {
                continue;
            }

            if (seenAt > bestTimestamp) {
                bestTimestamp = seenAt;
                bestCandidate = bossName;
            }
        }

        if (bestCandidate != null) {
            com.galyx.GALYX.LOGGER.info("[GALYX-ACTIONBAR] Item '{}' coincide con jefe cerca de morir '{}'", itemName, bestCandidate);
            nearDeathAt.remove(bestCandidate);
            confirmDefeat(bestCandidate, "loot tras vida baja (" + itemName + ")");
        }
    }

    private void confirmDefeat(String bossName, String reason) {
        if (DefeatGuard.tryConfirm(bossName)) {
            com.galyx.GALYX.LOGGER.info("[GALYX-ACTIONBAR] CONFIRMADO '{}' ({})", bossName, reason);
            eventBus.post(new CustomBossDefeatedEvent(bossName));
        } else {
            com.galyx.GALYX.LOGGER.info("[GALYX-ACTIONBAR] Bloqueado por DefeatGuard: '{}' ({})", bossName, reason);
        }
    }

    private String resolveKnownBoss(String rawName) {
        String upper = rawName.toUpperCase();
        for (String known : confirmationTextByBoss.keySet()) {
            if (upper.contains(known.toUpperCase())) {
                return known;
            }
        }
        return null;
    }

}