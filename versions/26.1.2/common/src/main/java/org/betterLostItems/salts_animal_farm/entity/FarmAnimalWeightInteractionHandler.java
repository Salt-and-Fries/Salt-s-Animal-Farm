package org.betterLostItems.salts_animal_farm.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;
import org.betterLostItems.salts_animal_farm.api.WeightedFarmAnimal;

public final class FarmAnimalWeightInteractionHandler {
    private FarmAnimalWeightInteractionHandler() {
    }

    public static InteractionResult interact(Player player, Level level, InteractionHand hand, Entity entity) {
        if (!Salts_animal_farm.CONFIG.modEnabled()
                || hand != InteractionHand.MAIN_HAND
                || !player.getItemInHand(hand).isEmpty()
                || player.isSpectator()
                || !(entity instanceof Animal animal)
                || !(entity instanceof WeightedFarmAnimal weightedAnimal)
                || !SaltsAnimalFarmConfigLists.isFarmAnimal(animal)) {
            return InteractionResult.PASS;
        }

        if (entity instanceof Leashable leashable && leashable.getLeashHolder() == player) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            showWeightActionBar(serverPlayer, weightedAnimal.salts_animal_farm$getWeight());
        }

        return InteractionResult.SUCCESS;
    }

    private static void showWeightActionBar(ServerPlayer player, int weight) {
        player.sendOverlayMessage(Component.literal("Weight: " + weight));
    }
}
