package org.betterLostItems.salts_animal_farm.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;
import org.betterLostItems.salts_animal_farm.debug.AnimalFarmDebugCommands;
import org.betterLostItems.salts_animal_farm.debug.AnimalFarmDebugDataSender;
import org.betterLostItems.salts_animal_farm.entity.FarmAnimalFearHandler;
import org.betterLostItems.salts_animal_farm.entity.FarmAnimalStateSender;
import org.betterLostItems.salts_animal_farm.entity.FarmAnimalWeightInteractionHandler;
import org.betterLostItems.salts_animal_farm.network.FarmAnimalStatePayload;
import org.betterLostItems.salts_animal_farm.network.FarmDebugDataPayload;
import org.betterLostItems.salts_animal_farm.network.RenderDebugFarmDataPayload;
import org.betterLostItems.salts_animal_farm.platform.SaltsAnimalFarmPlatform;

public final class SaltsAnimalFarmFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SaltsAnimalFarmPlatform.setClientboundPacketSender((player, payload) -> {
            if (!ServerPlayNetworking.canSend(player, payload.type())) {
                return false;
            }

            ServerPlayNetworking.send(player, payload);
            return true;
        });

        Salts_animal_farm.init(FabricLoader.getInstance().getConfigDir());
        registerPayloads();
        registerEvents();
    }

    private static void registerPayloads() {
        PayloadTypeRegistry.clientboundPlay().register(RenderDebugFarmDataPayload.TYPE, RenderDebugFarmDataPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FarmDebugDataPayload.TYPE, FarmDebugDataPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FarmAnimalStatePayload.TYPE, FarmAnimalStatePayload.CODEC);
    }

    private static void registerEvents() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                AnimalFarmDebugCommands.register(dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            FarmAnimalStateSender.onEndServerTick(server);
            AnimalFarmDebugDataSender.onEndServerTick(server);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            FarmAnimalStateSender.onPlayerDisconnect(handler.player);
            AnimalFarmDebugDataSender.onPlayerDisconnect(handler.player);
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) ->
                FarmAnimalFearHandler.afterDamage(entity, source, damageTaken));
        ServerLivingEntityEvents.AFTER_DEATH.register(FarmAnimalFearHandler::afterDeath);
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) ->
                FarmAnimalWeightInteractionHandler.interact(player, level, hand, entity));
    }
}
