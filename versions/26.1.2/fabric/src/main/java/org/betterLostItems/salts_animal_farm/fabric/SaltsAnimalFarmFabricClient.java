package org.betterLostItems.salts_animal_farm.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.betterLostItems.salts_animal_farm.client.AnimalFarmClientDebug;
import org.betterLostItems.salts_animal_farm.network.FarmAnimalStatePayload;
import org.betterLostItems.salts_animal_farm.network.FarmDebugDataPayload;
import org.betterLostItems.salts_animal_farm.network.RenderDebugFarmDataPayload;

public final class SaltsAnimalFarmFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(RenderDebugFarmDataPayload.TYPE, (payload, context) ->
                context.client().execute(() -> AnimalFarmClientDebug.setRenderDebugFarmData(payload.visible())));
        ClientPlayNetworking.registerGlobalReceiver(FarmDebugDataPayload.TYPE, (payload, context) ->
                context.client().execute(() -> AnimalFarmClientDebug.setEntityDebugData(payload.entries())));
        ClientPlayNetworking.registerGlobalReceiver(FarmAnimalStatePayload.TYPE, (payload, context) ->
                context.client().execute(() -> AnimalFarmClientDebug.setEntitySickStates(payload.entries())));
    }
}
