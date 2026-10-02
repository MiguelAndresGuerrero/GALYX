package com.galyx.client.stats;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cuenta cuantas "cabezas" de jefe de mundo tiene el jugador en el
 * inventario principal + hotbar (36 slots -- NO incluye armadura, offhand
 * ni ender chest). La comparacion es por el nombre mostrado del item
 * (getHoverName(), que devuelve el nombre custom si lo tiene o el nombre
 * normal del item si no), ignorando mayusculas/minusculas y espacios al
 * borde, contra el texto configurado en GalyxConfig#bossHeadNames.
 *
 * Es client-side puro: solo lee lo que el servidor ya le mando al cliente
 * para pintar el inventario, no requiere ningun paquete ni permiso extra.
 */
public final class InventoryHeadCounter {

    private InventoryHeadCounter() {
    }

    public static Map<String, Integer> count(Map<String, String> headNamesByBoss) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String bossName : headNamesByBoss.keySet()) {
            result.put(bossName, 0);
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return result;
        }

        // getContainerSize()/getItem(i) son parte de la interfaz Container
        // (metodos publicos, estables entre versiones) -- cubren exactamente
        // el inventario principal + hotbar (36 slots), SIN armadura ni
        // offhand, que tienen su propia lista aparte en Inventory.
        int size = player.getInventory().getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            String displayName = stack.getHoverName().getString().trim();
            for (Map.Entry<String, String> entry : headNamesByBoss.entrySet()) {
                String expected = entry.getValue();
                if (expected == null || expected.isBlank()) {
                    continue;
                }
                if (displayName.equalsIgnoreCase(expected.trim())) {
                    result.merge(entry.getKey(), stack.getCount(), Integer::sum);
                }
            }
        }

        return result;
    }
}