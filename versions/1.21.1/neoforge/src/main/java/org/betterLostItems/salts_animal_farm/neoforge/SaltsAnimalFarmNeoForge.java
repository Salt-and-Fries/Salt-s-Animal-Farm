package org.betterLostItems.salts_animal_farm.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;
import org.betterLostItems.salts_animal_farm.client.AnimalFarmClientDebug;
import org.betterLostItems.salts_animal_farm.debug.AnimalFarmDebugCommands;
import org.betterLostItems.salts_animal_farm.debug.AnimalFarmDebugDataSender;
import org.betterLostItems.salts_animal_farm.entity.FarmAnimalFearHandler;
import org.betterLostItems.salts_animal_farm.entity.FarmAnimalStateSender;
import org.betterLostItems.salts_animal_farm.entity.FarmAnimalWeightInteractionHandler;
import org.betterLostItems.salts_animal_farm.network.FarmAnimalStatePayload;
import org.betterLostItems.salts_animal_farm.network.FarmDebugDataPayload;
import org.betterLostItems.salts_animal_farm.network.OpenConfigScreenPayload;
import org.betterLostItems.salts_animal_farm.network.RenderDebugFarmDataPayload;
import org.betterLostItems.salts_animal_farm.network.SaltsAnimalFarmNetworking;
import org.betterLostItems.salts_animal_farm.platform.SaltsAnimalFarmPlatform;

@Mod(Salts_animal_farm.MOD_ID)
public final class SaltsAnimalFarmNeoForge {
    public SaltsAnimalFarmNeoForge(IEventBus modBus, ModContainer modContainer) {
        SaltsAnimalFarmPlatform.setClientboundPacketSender((player, payload) -> {
            PacketDistributor.sendToPlayer(player, payload);
            return true;
        });

        Salts_animal_farm.init(FMLPaths.CONFIGDIR.get());
        modBus.addListener(SaltsAnimalFarmNeoForge::registerPayloads);
        registerEvents();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            SaltsAnimalFarmNeoForgeClient.init(modBus, modContainer);
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(SaltsAnimalFarmNetworking.PROTOCOL_VERSION);
        registrar.playToClient(OpenConfigScreenPayload.TYPE, OpenConfigScreenPayload.CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (FMLEnvironment.dist == Dist.CLIENT) {
                        SaltsAnimalFarmNeoForgeClient.openConfigScreen();
                    }
                }));
        registrar.playToClient(RenderDebugFarmDataPayload.TYPE, RenderDebugFarmDataPayload.CODEC, (payload, context) ->
                context.enqueueWork(() -> AnimalFarmClientDebug.setRenderDebugFarmData(payload.visible())));
        registrar.playToClient(FarmDebugDataPayload.TYPE, FarmDebugDataPayload.CODEC, (payload, context) ->
                context.enqueueWork(() -> AnimalFarmClientDebug.setEntityDebugData(payload.entries())));
        registrar.playToClient(FarmAnimalStatePayload.TYPE, FarmAnimalStatePayload.CODEC, (payload, context) ->
                context.enqueueWork(() -> AnimalFarmClientDebug.setEntitySickStates(payload.entries())));
    }

    private static void registerEvents() {
        NeoForge.EVENT_BUS.addListener(SaltsAnimalFarmNeoForge::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(SaltsAnimalFarmNeoForge::onServerTick);
        NeoForge.EVENT_BUS.addListener(SaltsAnimalFarmNeoForge::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(SaltsAnimalFarmNeoForge::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(SaltsAnimalFarmNeoForge::onLivingDamage);
        NeoForge.EVENT_BUS.addListener(SaltsAnimalFarmNeoForge::onLivingDeath);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        AnimalFarmDebugCommands.register(event.getDispatcher());
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        FarmAnimalStateSender.onEndServerTick(event.getServer());
        AnimalFarmDebugDataSender.onEndServerTick(event.getServer());
    }

    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FarmAnimalStateSender.onPlayerDisconnect(player);
            AnimalFarmDebugDataSender.onPlayerDisconnect(player);
        }
    }

    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        InteractionResult result = FarmAnimalWeightInteractionHandler.interact(
                event.getEntity(),
                event.getLevel(),
                event.getHand(),
                event.getTarget()
        );

        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    private static void onLivingDamage(LivingDamageEvent.Post event) {
        FarmAnimalFearHandler.afterDamage(event.getEntity(), event.getSource(), event.getNewDamage());
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        FarmAnimalFearHandler.afterDeath(event.getEntity(), event.getSource());
    }
}
