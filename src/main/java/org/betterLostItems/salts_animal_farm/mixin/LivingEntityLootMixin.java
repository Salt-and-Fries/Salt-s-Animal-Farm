package org.betterLostItems.salts_animal_farm.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

import java.util.function.Consumer;

@Mixin(LivingEntity.class)
public abstract class LivingEntityLootMixin {
    @Inject(
            method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;ZLnet/minecraft/resources/ResourceKey;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void salts_animal_farm$dropWeightedFarmAnimalLoot(
            ServerLevel level,
            DamageSource source,
            boolean causedByPlayer,
            ResourceKey<LootTable> lootTableKey,
            Consumer<ItemStack> itemStackConsumer,
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

        LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(lootTableKey);
        LootParams params = createLootParams(level, source, causedByPlayer, entity);
        RandomSource random = createLootRandom(level, entity.getLootTableSeed());

        for (int i = 0; i < weight; i++) {
            lootTable.getRandomItems(params, random).forEach(itemStackConsumer);
        }
    }

    private static LootParams createLootParams(ServerLevel level, DamageSource source, boolean causedByPlayer, LivingEntity entity) {
        LootParams.Builder builder = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, entity)
                .withParameter(LootContextParams.ORIGIN, entity.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, source.getEntity())
                .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, source.getDirectEntity());

        Player killerPlayer = entity.getLastHurtByPlayer();

        if (causedByPlayer && killerPlayer != null) {
            builder = builder
                    .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killerPlayer)
                    .withLuck(killerPlayer.getLuck());
        }

        return builder.create(LootContextParamSets.ENTITY);
    }

    private static RandomSource createLootRandom(ServerLevel level, long lootTableSeed) {
        if (lootTableSeed != 0L) {
            return RandomSource.create(lootTableSeed);
        }

        return RandomSource.create(level.getRandom().nextLong());
    }
}
