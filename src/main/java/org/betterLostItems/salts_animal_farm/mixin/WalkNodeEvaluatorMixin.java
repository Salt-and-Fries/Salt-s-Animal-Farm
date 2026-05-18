package org.betterLostItems.salts_animal_farm.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;
import org.betterLostItems.salts_animal_farm.api.WeightedFarmAnimal;
import org.betterLostItems.salts_animal_farm.entity.AnimalWeatherComfort;
import org.betterLostItems.salts_animal_farm.entity.SaltsAnimalFarmConfigLists;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluatorMixin {
    @Inject(method = "getPathTypeOfMob", at = @At("HEAD"), cancellable = true)
    private void salts_animal_farm$keepShelteredAnimalsOnDryPaths(
            PathfindingContext context,
            int x,
            int y,
            int z,
            Mob mob,
            CallbackInfoReturnable<PathType> cir
    ) {
        if (!(mob instanceof Animal animal)
                || !SaltsAnimalFarmConfigLists.isFarmAnimal(animal)
                || !(animal instanceof WeightedFarmAnimal weightedAnimal)
                || weightedAnimal.salts_animal_farm$isFrantic()
                || animal.hurtTime > 0
                || !AnimalWeatherComfort.isRainingInRainBiome(animal)
                || !AnimalWeatherComfort.isFullyCovered(animal)
                || hasVisibleScaryMob(animal)) {
            return;
        }

        BlockPos pathPos = new BlockPos(x, y, z);
        if (AnimalWeatherComfort.isRainFallingAt(animal.level(), pathPos)) {
            cir.setReturnValue(PathType.BLOCKED);
        }
    }

    private boolean hasVisibleScaryMob(Animal animal) {
        AABB area = animal.getBoundingBox().inflate(Salts_animal_farm.CONFIG.hostileScareRadius());
        return animal.level().hasEntities(
                EntityTypeTest.forClass(LivingEntity.class),
                area,
                entity -> entity != animal
                        && entity.isAlive()
                        && SaltsAnimalFarmConfigLists.isScaryMob(entity)
                        && animal.getSensing().hasLineOfSight(entity)
        );
    }
}
