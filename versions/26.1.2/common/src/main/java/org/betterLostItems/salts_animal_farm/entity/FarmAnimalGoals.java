package org.betterLostItems.salts_animal_farm.entity;

import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.Animal;
import org.betterLostItems.salts_animal_farm.entity.ai.AnimalComfortGoal;
import org.betterLostItems.salts_animal_farm.entity.ai.FarmAnimalFleeHostilesGoal;
import org.betterLostItems.salts_animal_farm.entity.ai.FarmAnimalFranticGoal;

public final class FarmAnimalGoals {
    private FarmAnimalGoals() {
    }

    public static void addGoals(Animal animal, GoalSelector goalSelector) {
        goalSelector.addGoal(0, new FarmAnimalFranticGoal(animal));
        goalSelector.addGoal(1, new FarmAnimalFleeHostilesGoal(animal));
        goalSelector.addGoal(2, new AnimalComfortGoal(animal));
    }
}
