package org.betterLostItems.salts_animal_farm.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.betterLostItems.salts_animal_farm.client.AnimalFarmClientDebug;
import org.betterLostItems.salts_animal_farm.client.AnimalFarmClientScreens;
import org.betterLostItems.salts_animal_farm.network.FarmAnimalStatePayload;
import org.betterLostItems.salts_animal_farm.network.FarmDebugDataPayload;
import org.betterLostItems.salts_animal_farm.network.OpenConfigScreenPayload;
import org.betterLostItems.salts_animal_farm.network.RenderDebugFarmDataPayload;

public final class SaltsAnimalFarmFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(OpenConfigScreenPayload.ID, (client, handler, buffer, responseSender) -> {
            OpenConfigScreenPayload.decode(buffer);
            client.execute(AnimalFarmClientScreens::openConfigScreen);
        });
        ClientPlayNetworking.registerGlobalReceiver(RenderDebugFarmDataPayload.ID, (client, handler, buffer, responseSender) -> {
            RenderDebugFarmDataPayload payload = RenderDebugFarmDataPayload.decode(buffer);
            client.execute(() -> AnimalFarmClientDebug.setRenderDebugFarmData(payload.visible()));
        });
        ClientPlayNetworking.registerGlobalReceiver(FarmDebugDataPayload.ID, (client, handler, buffer, responseSender) -> {
            FarmDebugDataPayload payload = FarmDebugDataPayload.decode(buffer);
            client.execute(() -> AnimalFarmClientDebug.setEntityDebugData(payload.entries()));
        });
        ClientPlayNetworking.registerGlobalReceiver(FarmAnimalStatePayload.ID, (client, handler, buffer, responseSender) -> {
            FarmAnimalStatePayload payload = FarmAnimalStatePayload.decode(buffer);
            client.execute(() -> AnimalFarmClientDebug.setEntitySickStates(payload.entries()));
        });
    }
}
