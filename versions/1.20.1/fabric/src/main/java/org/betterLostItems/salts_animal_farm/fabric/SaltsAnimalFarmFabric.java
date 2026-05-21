package org.betterLostItems.salts_animal_farm.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.FriendlyByteBuf;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;
import org.betterLostItems.salts_animal_farm.debug.AnimalFarmDebugCommands;
import org.betterLostItems.salts_animal_farm.debug.AnimalFarmDebugDataSender;
import org.betterLostItems.salts_animal_farm.entity.FarmAnimalFearHandler;
import org.betterLostItems.salts_animal_farm.entity.FarmAnimalStateSender;
import org.betterLostItems.salts_animal_farm.entity.FarmAnimalWeightInteractionHandler;
import org.betterLostItems.salts_animal_farm.network.FarmAnimalStatePayload;
import org.betterLostItems.salts_animal_farm.network.FarmDebugDataPayload;
import org.betterLostItems.salts_animal_farm.network.OpenConfigScreenPayload;
import org.betterLostItems.salts_animal_farm.network.RenderDebugFarmDataPayload;
import org.betterLostItems.salts_animal_farm.platform.SaltsAnimalFarmPlatform;

public final class SaltsAnimalFarmFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SaltsAnimalFarmPlatform.setClientboundPacketSender((player, payload) -> {
            if (payload instanceof OpenConfigScreenPayload openConfigScreenPayload) {
                return send(player, OpenConfigScreenPayload.ID, buffer(openConfigScreenPayload));
            }
            if (payload instanceof RenderDebugFarmDataPayload renderDebugFarmDataPayload) {
                return send(player, RenderDebugFarmDataPayload.ID, buffer(renderDebugFarmDataPayload));
            }
            if (payload instanceof FarmDebugDataPayload farmDebugDataPayload) {
                return send(player, FarmDebugDataPayload.ID, buffer(farmDebugDataPayload));
            }
            if (payload instanceof FarmAnimalStatePayload farmAnimalStatePayload) {
                return send(player, FarmAnimalStatePayload.ID, buffer(farmAnimalStatePayload));
            }

            return false;
        });

        Salts_animal_farm.init(FabricLoader.getInstance().getConfigDir());
        registerEvents();
    }

    private static boolean send(net.minecraft.server.level.ServerPlayer player, net.minecraft.resources.ResourceLocation channel, FriendlyByteBuf buffer) {
        if (!ServerPlayNetworking.canSend(player, channel)) {
            return false;
        }

        ServerPlayNetworking.send(player, channel, buffer);
        return true;
    }

    private static FriendlyByteBuf buffer(OpenConfigScreenPayload payload) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        OpenConfigScreenPayload.encode(payload, buffer);
        return buffer;
    }

    private static FriendlyByteBuf buffer(RenderDebugFarmDataPayload payload) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        RenderDebugFarmDataPayload.encode(payload, buffer);
        return buffer;
    }

    private static FriendlyByteBuf buffer(FarmDebugDataPayload payload) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        FarmDebugDataPayload.encode(payload, buffer);
        return buffer;
    }

    private static FriendlyByteBuf buffer(FarmAnimalStatePayload payload) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        FarmAnimalStatePayload.encode(payload, buffer);
        return buffer;
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
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            FarmAnimalFearHandler.afterDamage(entity, source, amount);
            return true;
        });
        ServerLivingEntityEvents.AFTER_DEATH.register(FarmAnimalFearHandler::afterDeath);
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) ->
                FarmAnimalWeightInteractionHandler.interact(player, level, hand, entity));
    }
}
