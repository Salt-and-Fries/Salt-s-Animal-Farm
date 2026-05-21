package org.betterLostItems.salts_animal_farm.entity.ai;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;
import org.betterLostItems.salts_animal_farm.api.WeightedFarmAnimal;
import org.betterLostItems.salts_animal_farm.config.SaltsAnimalFarmConfig;
import org.betterLostItems.salts_animal_farm.entity.SaltsAnimalFarmConfigLists;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class FarmAnimalFleeHostilesGoal extends Goal {
    private final Animal animal;
    private final WeightedFarmAnimal weightedAnimal;
    private final List<LivingEntity> nearbyHostiles = new ArrayList<>(12);
    private LivingEntity hostile;
    private Vec3 fleePos;
    private int nextScanTick;

    public FarmAnimalFleeHostilesGoal(Animal animal) {
        this.animal = animal;
        this.weightedAnimal = (WeightedFarmAnimal) animal;
        setFlags(EnumSet.of(Flag.MOVE));
        scheduleNextScan();
    }

    @Override
    public boolean canUse() {
        if (!SaltsAnimalFarmConfigLists.isFarmAnimal(animal) || animal.tickCount < nextScanTick) {
            return false;
        }

        scheduleNextScan();
        hostile = findNearestHostile();

        if (hostile == null) {
            return false;
        }

        fleePos = LandRandomPos.getPosAway(animal, 16, 7, hostile.position());

        if (fleePos == null) {
            return false;
        }

        weightedAnimal.salts_animal_farm$tryHostileScare(Salts_animal_farm.CONFIG.hostileScareCooldownTicks());
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return hostile != null && hostile.isAlive() && !animal.getNavigation().isDone();
    }

    @Override
    public void start() {
        animal.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, Salts_animal_farm.CONFIG.hostileFleeSpeed());
    }

    @Override
    public void stop() {
        hostile = null;
        fleePos = null;
    }

    private LivingEntity findNearestHostile() {
        int radius = Salts_animal_farm.CONFIG.effectiveValues(animal.level()).hostileScareRadius();
        AABB area = animal.getBoundingBox().inflate(radius);
        nearbyHostiles.clear();
        animal.level().getEntities(
                EntityTypeTest.forClass(LivingEntity.class),
                area,
                entity -> entity != animal && entity.isAlive() && SaltsAnimalFarmConfigLists.isScaryMob(entity) && animal.getSensing().hasLineOfSight(entity),
                nearbyHostiles,
                12
        );

        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (LivingEntity candidate : nearbyHostiles) {
            double distance = candidate.distanceToSqr(animal);

            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }

        nearbyHostiles.clear();
        return nearest;
    }

    private void scheduleNextScan() {
        SaltsAnimalFarmConfig config = Salts_animal_farm.CONFIG;
        int randomOffset = config.hostileScanRandomOffsetTicks() <= 0 ? 0 : animal.getRandom().nextInt(config.hostileScanRandomOffsetTicks() + 1);
        nextScanTick = animal.tickCount + config.hostileScanIntervalTicks() + randomOffset;
    }
}
