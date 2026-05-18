package org.betterLostItems.salts_animal_farm.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.betterLostItems.salts_animal_farm.network.FarmDebugDataPayload;
import org.betterLostItems.salts_animal_farm.network.RenderDebugFarmDataPayload;

public class Salts_animal_farmClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(RenderDebugFarmDataPayload.TYPE, (payload, context) ->
                context.client().execute(() -> AnimalFarmClientDebug.setRenderDebugFarmData(payload.visible())));
        ClientPlayNetworking.registerGlobalReceiver(FarmDebugDataPayload.TYPE, (payload, context) ->
                context.client().execute(() -> AnimalFarmClientDebug.setEntityDebugData(payload.entries())));
    }
}
