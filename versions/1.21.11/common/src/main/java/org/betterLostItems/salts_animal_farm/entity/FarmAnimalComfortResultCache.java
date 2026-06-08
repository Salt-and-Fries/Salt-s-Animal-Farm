package org.betterLostItems.salts_animal_farm.entity;

import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class FarmAnimalComfortResultCache {
    public static final int RECENT_RESULT_TICKS = 6000;
    private static final Map<Animal, Entry> RECENT_DIRECT_RESULTS = new WeakHashMap<>();

    private FarmAnimalComfortResultCache() {
    }

    public static void recordDirectResult(Animal animal, String taskName, boolean success) {
        RECENT_DIRECT_RESULTS.put(animal, new Entry(
                animal.getUUID(),
                animal.getId(),
                animal.level().getGameTime(),
                cleanTaskName(taskName),
                success
        ));
    }

    public static void clearDirectResult(Animal animal) {
        RECENT_DIRECT_RESULTS.remove(animal);
    }

    public static Entry findRecentDirectResultToCopy(Animal animal, int radius) {
        if (radius <= 0) {
            return null;
        }

        long currentTick = animal.level().getGameTime();
        AABB area = animal.getBoundingBox().inflate(radius);
        List<Animal> nearbyAnimals = animal.level().getEntities(
                EntityTypeTest.forClass(Animal.class),
                area,
                nearbyAnimal -> nearbyAnimal != animal
                        && nearbyAnimal.isAlive()
                        && SaltsAnimalFarmConfigLists.isManagedFarmAnimal(nearbyAnimal)
        );
        Entry best = null;
        double bestDistance = Double.MAX_VALUE;

        for (Animal nearbyAnimal : nearbyAnimals) {
            Entry entry = RECENT_DIRECT_RESULTS.get(nearbyAnimal);

            if (entry == null) {
                continue;
            }

            if (!entry.isFresh(currentTick)) {
                RECENT_DIRECT_RESULTS.remove(nearbyAnimal);
                continue;
            }

            double distance = nearbyAnimal.distanceToSqr(animal);
            if (distance < bestDistance) {
                best = entry;
                bestDistance = distance;
            }
        }

        return best;
    }

    private static String cleanTaskName(String taskName) {
        return taskName == null || taskName.isBlank() ? "Null" : taskName;
    }

    public record Entry(UUID sourceUuid, int sourceEntityId, long completedTick, String taskName, boolean success) {
        private boolean isFresh(long currentTick) {
            long age = currentTick - completedTick;
            return age >= 0L && age <= RECENT_RESULT_TICKS;
        }
    }
}
