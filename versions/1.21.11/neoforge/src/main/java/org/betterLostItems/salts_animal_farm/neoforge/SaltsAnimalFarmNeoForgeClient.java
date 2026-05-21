package org.betterLostItems.salts_animal_farm.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import org.betterLostItems.salts_animal_farm.client.AnimalFarmClientDebug;
import org.betterLostItems.salts_animal_farm.client.AnimalFarmClientScreens;
import org.betterLostItems.salts_animal_farm.client.config.SaltsAnimalFarmConfigScreen;
import org.betterLostItems.salts_animal_farm.network.FarmAnimalStatePayload;
import org.betterLostItems.salts_animal_farm.network.FarmDebugDataPayload;
import org.betterLostItems.salts_animal_farm.network.OpenConfigScreenPayload;
import org.betterLostItems.salts_animal_farm.network.RenderDebugFarmDataPayload;

public final class SaltsAnimalFarmNeoForgeClient {
    private SaltsAnimalFarmNeoForgeClient() {
    }

    public static void init(IEventBus modBus, ModContainer modContainer) {
        modBus.addListener(SaltsAnimalFarmNeoForgeClient::registerPayloadHandlers);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (container, parent) -> new SaltsAnimalFarmConfigScreen(parent));
    }

    private static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(OpenConfigScreenPayload.TYPE, (payload, context) ->
                context.enqueueWork(AnimalFarmClientScreens::openConfigScreen));
        event.register(RenderDebugFarmDataPayload.TYPE, (payload, context) ->
                context.enqueueWork(() -> AnimalFarmClientDebug.setRenderDebugFarmData(payload.visible())));
        event.register(FarmDebugDataPayload.TYPE, (payload, context) ->
                context.enqueueWork(() -> AnimalFarmClientDebug.setEntityDebugData(payload.entries())));
        event.register(FarmAnimalStatePayload.TYPE, (payload, context) ->
                context.enqueueWork(() -> AnimalFarmClientDebug.setEntitySickStates(payload.entries())));
    }
}
