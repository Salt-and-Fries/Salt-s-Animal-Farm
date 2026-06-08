package org.betterLostItems.salts_animal_farm.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;

import java.util.Map;
import java.util.WeakHashMap;

public final class FarmAnimalThreatCache {
    private static final Map<Animal, VisibleThreatCache> VISIBLE_THREAT_CACHE = new WeakHashMap<>();

    private FarmAnimalThreatCache() {
    }

    public static boolean hasVisibleScaryMob(Animal animal) {
        int radius = Salts_animal_farm.CONFIG.effectiveValues(animal.level()).hostileScareRadius();
        long tick = animal.level().getGameTime();
        VisibleThreatCache cached = VISIBLE_THREAT_CACHE.get(animal);

        if (cached != null && cached.tick == tick && cached.radius == radius) {
            return cached.result;
        }

        AABB area = animal.getBoundingBox().inflate(radius);
        boolean result = !animal.level().getEntities(
                EntityTypeTest.forClass(LivingEntity.class),
                area,
                entity -> entity != animal
                        && entity.isAlive()
                        && SaltsAnimalFarmConfigLists.isScaryMob(entity)
                        && animal.getSensing().hasLineOfSight(entity)
        ).isEmpty();
        VISIBLE_THREAT_CACHE.put(animal, new VisibleThreatCache(tick, radius, result));
        return result;
    }

    private record VisibleThreatCache(long tick, int radius, boolean result) {
    }
}
