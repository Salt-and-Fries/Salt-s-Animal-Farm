package org.betterLostItems.salts_animal_farm.client;

import net.minecraft.network.chat.Component;
import org.betterLostItems.salts_animal_farm.network.FarmAnimalStatePayload;
import org.betterLostItems.salts_animal_farm.network.FarmDebugDataPayload;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AnimalFarmClientDebug {
    private static final Map<Integer, List<Component>> ENTITY_DEBUG_LINES = new HashMap<>();
    private static final Map<Integer, Boolean> ENTITY_SICK_STATES = new HashMap<>();
    private static boolean renderDebugFarmData;

    private AnimalFarmClientDebug() {
    }

    public static boolean shouldRenderDebugFarmData() {
        return renderDebugFarmData;
    }

    public static void setRenderDebugFarmData(boolean visible) {
        renderDebugFarmData = visible;

        if (!visible) {
            ENTITY_DEBUG_LINES.clear();
        }
    }

    public static boolean isEntitySick(int entityId) {
        return ENTITY_SICK_STATES.getOrDefault(entityId, false);
    }

    public static void setEntityDebugData(List<FarmDebugDataPayload.Entry> entries) {
        ENTITY_DEBUG_LINES.clear();

        for (FarmDebugDataPayload.Entry entry : entries) {
            ENTITY_DEBUG_LINES.put(
                    entry.entityId(),
                    entry.lines().stream().<Component>map(Component::literal).toList()
            );
        }
    }

    public static List<Component> getEntityDebugLines(int entityId) {
        return ENTITY_DEBUG_LINES.getOrDefault(entityId, List.of());
    }

    public static void setEntitySickStates(List<FarmAnimalStatePayload.Entry> entries) {
        ENTITY_SICK_STATES.clear();

        for (FarmAnimalStatePayload.Entry entry : entries) {
            if (entry.sick()) {
                ENTITY_SICK_STATES.put(entry.entityId(), true);
            }
        }
    }
}
