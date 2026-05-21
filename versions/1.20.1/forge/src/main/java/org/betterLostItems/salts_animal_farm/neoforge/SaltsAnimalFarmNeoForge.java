package org.betterLostItems.salts_animal_farm.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;
import org.betterLostItems.salts_animal_farm.debug.AnimalFarmDebugCommands;
import org.betterLostItems.salts_animal_farm.debug.AnimalFarmDebugDataSender;
import org.betterLostItems.salts_animal_farm.entity.FarmAnimalFearHandler;
import org.betterLostItems.salts_animal_farm.entity.FarmAnimalStateSender;
import org.betterLostItems.salts_animal_farm.entity.FarmAnimalWeightInteractionHandler;
import org.betterLostItems.salts_animal_farm.platform.SaltsAnimalFarmPlatform;

@Mod(Salts_animal_farm.MOD_ID)
public final class SaltsAnimalFarmNeoForge {
    public SaltsAnimalFarmNeoForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        SaltsAnimalFarmForgeNetworking.register();
        SaltsAnimalFarmPlatform.setClientboundPacketSender(SaltsAnimalFarmForgeNetworking::sendToPlayer);
        Salts_animal_farm.init(FMLPaths.CONFIGDIR.get());
        registerEvents();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            SaltsAnimalFarmNeoForgeClient.init(modBus);
        }
    }

    private static void registerEvents() {
        MinecraftForge.EVENT_BUS.addListener(SaltsAnimalFarmNeoForge::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(SaltsAnimalFarmNeoForge::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(SaltsAnimalFarmNeoForge::onPlayerLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(SaltsAnimalFarmNeoForge::onEntityInteract);
        MinecraftForge.EVENT_BUS.addListener(SaltsAnimalFarmNeoForge::onLivingDamage);
        MinecraftForge.EVENT_BUS.addListener(SaltsAnimalFarmNeoForge::onLivingDeath);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        AnimalFarmDebugCommands.register(event.getDispatcher());
    }

    private static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

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

    private static void onLivingDamage(LivingDamageEvent event) {
        FarmAnimalFearHandler.afterDamage(event.getEntity(), event.getSource(), event.getAmount());
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        FarmAnimalFearHandler.afterDeath(event.getEntity(), event.getSource());
    }
}
