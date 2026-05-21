package org.betterLostItems.salts_animal_farm.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import org.betterLostItems.salts_animal_farm.network.FarmAnimalStatePayload;
import org.betterLostItems.salts_animal_farm.network.FarmDebugDataPayload;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class AnimalFarmClientDebug {
    private static final Map<EntityRenderState, List<Component>> DEBUG_LINES = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<EntityRenderState, Boolean> SICK_STATES = Collections.synchronizedMap(new WeakHashMap<>());
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
            DEBUG_LINES.clear();
        }
    }

    public static void setSick(EntityRenderState state, boolean sick) {
        if (sick) {
            SICK_STATES.put(state, true);
        } else {
            SICK_STATES.remove(state);
        }
    }

    public static boolean isSick(EntityRenderState state) {
        return SICK_STATES.getOrDefault(state, false);
    }

    public static boolean isEntitySick(int entityId) {
        return ENTITY_SICK_STATES.getOrDefault(entityId, false);
    }

    public static void clearSick(EntityRenderState state) {
        SICK_STATES.remove(state);
    }

    public static void setDebugLines(EntityRenderState state, List<Component> lines) {
        if (lines.isEmpty()) {
            DEBUG_LINES.remove(state);
            return;
        }

        DEBUG_LINES.put(state, List.copyOf(lines));
    }

    public static List<Component> getDebugLines(EntityRenderState state) {
        return DEBUG_LINES.getOrDefault(state, List.of());
    }

    public static void clearDebugLines(EntityRenderState state) {
        DEBUG_LINES.remove(state);
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
