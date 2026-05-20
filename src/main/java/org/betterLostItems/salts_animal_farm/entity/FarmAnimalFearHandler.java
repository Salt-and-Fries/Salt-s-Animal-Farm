package org.betterLostItems.salts_animal_farm.entity;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;
import org.betterLostItems.salts_animal_farm.api.WeightedFarmAnimal;

import java.util.ArrayList;
import java.util.List;

public final class FarmAnimalFearHandler {
    private FarmAnimalFearHandler() {
    }

    public static void register() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register(FarmAnimalFearHandler::afterDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register(FarmAnimalFearHandler::afterDeath);
    }

    private static void afterDamage(LivingEntity entity, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        if (!(entity instanceof Animal animal) || !entity.isAlive() || damageTaken <= 0.0F || !SaltsAnimalFarmConfigLists.isFarmAnimal(animal)) {
            return;
        }

        if (source.getEntity() instanceof Player && Salts_animal_farm.CONFIG.effectiveValues(animal.level()).loseWeightWhenHitByPlayer()) {
            frighten(animal, 2, Salts_animal_farm.CONFIG.franticDurationTicks());
        }
    }

    private static void afterDeath(LivingEntity entity, DamageSource source) {
        if (!(entity.level() instanceof ServerLevel level) || !(entity instanceof Animal killedAnimal) || !SaltsAnimalFarmConfigLists.isFarmAnimal(killedAnimal)) {
            return;
        }

        if (source.getEntity() instanceof Player && Salts_animal_farm.CONFIG.effectiveValues(level).loseWeightWhenWitnessingAnimalDeath()) {
            panicWitnesses(level, killedAnimal);
        }
    }

    private static void panicWitnesses(ServerLevel level, Animal killedAnimal) {
        int radius = Salts_animal_farm.CONFIG.killWitnessRadius();
        AABB area = killedAnimal.getBoundingBox().inflate(radius);
        List<Animal> witnesses = new ArrayList<>();
        level.getEntities(
                EntityTypeTest.forClass(Animal.class),
                area,
                witness -> witness != killedAnimal && witness.isAlive() && SaltsAnimalFarmConfigLists.isFarmAnimal(witness),
                witnesses,
                Salts_animal_farm.CONFIG.maxKillWitnesses()
        );

        for (Animal witness : witnesses) {
            if (witness.getSensing().hasLineOfSight(killedAnimal)) {
                frighten(witness, 2, Salts_animal_farm.CONFIG.franticDurationTicks());
            }
        }
    }

    public static void frighten(Animal animal, int weightLoss, int franticTicks) {
        if (!(animal instanceof WeightedFarmAnimal weightedAnimal)) {
            return;
        }

        weightedAnimal.salts_animal_farm$addWeight(-weightLoss);
        weightedAnimal.salts_animal_farm$resetComfortStreak();
        weightedAnimal.salts_animal_farm$startFrantic(franticTicks);
    }

    public static boolean isPlayerCaused(DamageSource source) {
        Entity sourceEntity = source.getEntity();
        return sourceEntity instanceof Player;
    }
}
