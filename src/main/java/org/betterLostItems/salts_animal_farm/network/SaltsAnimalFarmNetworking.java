package org.betterLostItems.salts_animal_farm.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class SaltsAnimalFarmNetworking {
    private static boolean registered;

    private SaltsAnimalFarmNetworking() {
    }

    public static void registerPayloads() {
        if (registered) {
            return;
        }

        registered = true;
        PayloadTypeRegistry.clientboundPlay().register(RenderDebugFarmDataPayload.TYPE, RenderDebugFarmDataPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FarmDebugDataPayload.TYPE, FarmDebugDataPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FarmAnimalStatePayload.TYPE, FarmAnimalStatePayload.CODEC);
    }
}
