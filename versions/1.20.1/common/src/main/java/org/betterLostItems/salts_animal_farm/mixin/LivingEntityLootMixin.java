package org.betterLostItems.salts_animal_farm.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.betterLostItems.salts_animal_farm.api.WeightedFarmAnimal;
import org.betterLostItems.salts_animal_farm.entity.SaltsAnimalFarmConfigLists;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityLootMixin {
    @Inject(method = "getExperienceReward", at = @At("HEAD"), cancellable = true)
    private void salts_animal_farm$preventSickFarmAnimalExperience(CallbackInfoReturnable<Integer> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (entity instanceof Animal animal
                && entity instanceof WeightedFarmAnimal weightedAnimal
                && SaltsAnimalFarmConfigLists.isFarmAnimal(animal)
                && weightedAnimal.salts_animal_farm$isSick()) {
            cir.setReturnValue(0);
        }
    }

    @Inject(
            method = "dropFromLootTable",
            at = @At("HEAD"),
            cancellable = true
    )
    private void salts_animal_farm$dropWeightedFarmAnimalLoot(
            DamageSource source,
            boolean causedByPlayer,
            CallbackInfo ci
    ) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (!(entity instanceof Animal animal) || !(entity instanceof WeightedFarmAnimal weightedAnimal) || !SaltsAnimalFarmConfigLists.isFarmAnimal(animal)) {
            return;
        }

        ci.cancel();

        int weight = weightedAnimal.salts_animal_farm$getWeight();

        if (weight <= 0) {
            return;
        }

        ServerLevel level = (ServerLevel) entity.level();
        LootTable lootTable = level.getServer().getLootData().getLootTable(entity.getLootTable());
        LootParams params = createLootParams(level, source, causedByPlayer, entity);

        for (int i = 0; i < weight; i++) {
            lootTable.getRandomItems(params, entity.getLootTableSeed(), entity::spawnAtLocation);
        }
    }

    private static LootParams createLootParams(ServerLevel level, DamageSource source, boolean causedByPlayer, LivingEntity entity) {
        LootParams.Builder builder = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, entity)
                .withParameter(LootContextParams.ORIGIN, entity.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withOptionalParameter(LootContextParams.KILLER_ENTITY, source.getEntity())
                .withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, source.getDirectEntity());

        Player killerPlayer = source.getEntity() instanceof Player player ? player : null;

        if (causedByPlayer && killerPlayer != null) {
            builder = builder
                    .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killerPlayer)
                    .withLuck(killerPlayer.getLuck());
        }

        return builder.create(LootContextParamSets.ENTITY);
    }
}
