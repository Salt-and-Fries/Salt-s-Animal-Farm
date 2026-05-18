package org.betterLostItems.salts_animal_farm.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import org.betterLostItems.salts_animal_farm.entity.FarmAnimalGoals;
import org.betterLostItems.salts_animal_farm.entity.SaltsAnimalFarmConfigLists;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobMixin {
    @Shadow
    @Final
    protected GoalSelector goalSelector;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void salts_animal_farm$addFarmAnimalGoals(EntityType<? extends Mob> entityType, Level level, CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;

        if (level instanceof ServerLevel && mob instanceof Animal animal && SaltsAnimalFarmConfigLists.isFarmAnimal(animal)) {
            FarmAnimalGoals.addGoals(animal, goalSelector);
        }
    }
}
