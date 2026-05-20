package org.betterLostItems.salts_animal_farm.entity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;
import org.betterLostItems.salts_animal_farm.api.WeightedFarmAnimal;
import org.betterLostItems.salts_animal_farm.network.FarmAnimalStatePayload;

import java.util.ArrayList;
import java.util.List;

public final class FarmAnimalStateSender {
    private static final int SEND_INTERVAL_TICKS = 10;
    private static final double STATE_RADIUS = 64.0D;
    private static int tickCounter;
    private static boolean registered;

    private FarmAnimalStateSender() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;

            if (tickCounter % SEND_INTERVAL_TICKS != 0) {
                return;
            }

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (ServerPlayNetworking.canSend(player, FarmAnimalStatePayload.TYPE)) {
                    ServerPlayNetworking.send(player, buildPayload(player));
                }
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (ServerPlayNetworking.canSend(handler.player, FarmAnimalStatePayload.TYPE)) {
                ServerPlayNetworking.send(handler.player, new FarmAnimalStatePayload(List.of()));
            }
        });
    }

    private static FarmAnimalStatePayload buildPayload(ServerPlayer player) {
        if (!Salts_animal_farm.CONFIG.modEnabled()) {
            return new FarmAnimalStatePayload(List.of());
        }

        AABB area = player.getBoundingBox().inflate(STATE_RADIUS);
        List<Animal> animals = player.level().getEntities(
                EntityTypeTest.forClass(Animal.class),
                area,
                animal -> animal.isAlive() && animal instanceof WeightedFarmAnimal && SaltsAnimalFarmConfigLists.isFarmAnimal(animal)
        );
        List<FarmAnimalStatePayload.Entry> entries = new ArrayList<>(animals.size());

        for (Animal animal : animals) {
            WeightedFarmAnimal weightedAnimal = (WeightedFarmAnimal) animal;
            entries.add(new FarmAnimalStatePayload.Entry(animal.getId(), weightedAnimal.salts_animal_farm$isSick()));
        }

        return new FarmAnimalStatePayload(entries);
    }
}
