package org.betterLostItems.salts_animal_farm.entity;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;
import org.betterLostItems.salts_animal_farm.api.WeightedFarmAnimal;
import org.betterLostItems.salts_animal_farm.network.FarmAnimalStatePayload;
import org.betterLostItems.salts_animal_farm.platform.SaltsAnimalFarmPlatform;

import java.util.ArrayList;
import java.util.List;

public final class FarmAnimalStateSender {
    private static final int SEND_INTERVAL_TICKS = 10;
    private static final double STATE_RADIUS = 64.0D;
    private static int tickCounter;

    private FarmAnimalStateSender() {
    }

    public static void onEndServerTick(MinecraftServer server) {
        tickCounter++;

        if (tickCounter % SEND_INTERVAL_TICKS != 0) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            SaltsAnimalFarmPlatform.trySend(player, buildPayload(player));
        }
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        SaltsAnimalFarmPlatform.trySend(player, new FarmAnimalStatePayload(List.of()));
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
