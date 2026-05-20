package org.betterLostItems.salts_animal_farm.debug;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.betterLostItems.salts_animal_farm.api.WeightedFarmAnimal;
import org.betterLostItems.salts_animal_farm.entity.SaltsAnimalFarmConfigLists;
import org.betterLostItems.salts_animal_farm.network.FarmDebugDataPayload;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class AnimalFarmDebugDataSender {
    private static final int SEND_INTERVAL_TICKS = 1;
    private static final double DEBUG_RADIUS = 48.0D;
    private static final Set<UUID> ENABLED_PLAYERS = new HashSet<>();
    private static int tickCounter;
    private static boolean registered;

    private AnimalFarmDebugDataSender() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;

            if (tickCounter % SEND_INTERVAL_TICKS != 0 || ENABLED_PLAYERS.isEmpty()) {
                return;
            }

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (ENABLED_PLAYERS.contains(player.getUUID()) && ServerPlayNetworking.canSend(player, FarmDebugDataPayload.TYPE)) {
                    ServerPlayNetworking.send(player, buildPayload(player));
                }
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ENABLED_PLAYERS.remove(handler.player.getUUID()));
    }

    public static void setEnabled(ServerPlayer player, boolean enabled) {
        if (!ServerPlayNetworking.canSend(player, FarmDebugDataPayload.TYPE)) {
            return;
        }

        if (enabled) {
            ENABLED_PLAYERS.add(player.getUUID());
            ServerPlayNetworking.send(player, buildPayload(player));
        } else {
            ENABLED_PLAYERS.remove(player.getUUID());
            ServerPlayNetworking.send(player, new FarmDebugDataPayload(List.of()));
        }
    }

    private static FarmDebugDataPayload buildPayload(ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(DEBUG_RADIUS);
        List<Animal> animals = player.level().getEntities(
                EntityTypeTest.forClass(Animal.class),
                area,
                animal -> animal.isAlive() && animal instanceof WeightedFarmAnimal && SaltsAnimalFarmConfigLists.isFarmAnimal(animal)
        );
        List<FarmDebugDataPayload.Entry> entries = new ArrayList<>(animals.size());

        for (Animal animal : animals) {
            WeightedFarmAnimal weightedAnimal = (WeightedFarmAnimal) animal;
            entries.add(new FarmDebugDataPayload.Entry(animal.getId(), debugLines(animal, weightedAnimal)));
        }

        return new FarmDebugDataPayload(entries);
    }

    private static List<String> debugLines(Animal animal, WeightedFarmAnimal weightedAnimal) {
        String currentTask = cleanDebugValue(weightedAnimal.salts_animal_farm$getCurrentComfortTask());
        String lastTask = cleanDebugValue(weightedAnimal.salts_animal_farm$getLastComfortTask());
        String lastResult = cleanDebugValue(weightedAnimal.salts_animal_farm$getLastComfortTaskResult());
        return List.of(
                "Farm Debug: " + animal.getType().toShortString(),
                "Weight: " + weightedAnimal.salts_animal_farm$getWeight() + " | Age: " + animal.getAge(),
                "Task: " + currentTask + " | Last: " + lastTask + " " + lastResult,
                "Streak: +" + weightedAnimal.salts_animal_farm$getSuccessfulTaskStreak()
                        + " / -" + weightedAnimal.salts_animal_farm$getFailedTaskStreak()
                        + " | Success: " + weightedAnimal.salts_animal_farm$getTotalSuccessfulTasks()
                        + " | Fail: " + weightedAnimal.salts_animal_farm$getTotalFailedTasks(),
                "Frantic: " + weightedAnimal.salts_animal_farm$getFranticTicks()
                        + " | Scare CD: " + weightedAnimal.salts_animal_farm$getScareCooldownTicks(),
                "Rain Exposure: " + weightedAnimal.salts_animal_farm$getRainExposureTicks()
        );
    }

    private static String cleanDebugValue(String value) {
        return value == null || value.isBlank() ? "Null" : value;
    }
}
