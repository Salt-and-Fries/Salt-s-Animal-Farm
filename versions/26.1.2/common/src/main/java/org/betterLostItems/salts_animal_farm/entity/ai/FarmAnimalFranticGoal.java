package org.betterLostItems.salts_animal_farm.entity.ai;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;
import org.betterLostItems.salts_animal_farm.api.WeightedFarmAnimal;
import org.betterLostItems.salts_animal_farm.entity.SaltsAnimalFarmConfigLists;

import java.util.EnumSet;

public class FarmAnimalFranticGoal extends Goal {
    private final Animal animal;
    private final WeightedFarmAnimal weightedAnimal;
    private int repathTicks;

    public FarmAnimalFranticGoal(Animal animal) {
        this.animal = animal;
        this.weightedAnimal = (WeightedFarmAnimal) animal;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return SaltsAnimalFarmConfigLists.isFarmAnimal(animal) && weightedAnimal.salts_animal_farm$isFrantic();
    }

    @Override
    public boolean canContinueToUse() {
        return weightedAnimal.salts_animal_farm$isFrantic();
    }

    @Override
    public void start() {
        repathTicks = 0;
        moveSomewhere();
    }

    @Override
    public void tick() {
        repathTicks--;

        if (repathTicks <= 0 || animal.getNavigation().isDone()) {
            moveSomewhere();
        }
    }

    @Override
    public void stop() {
        animal.getNavigation().stop();
    }

    private void moveSomewhere() {
        repathTicks = Salts_animal_farm.CONFIG.franticRepathTicks() + animal.getRandom().nextInt(Salts_animal_farm.CONFIG.franticRepathTicks() + 1);
        Vec3 target = LandRandomPos.getPos(animal, 10, 5);

        if (target != null) {
            animal.getNavigation().moveTo(target.x, target.y, target.z, Salts_animal_farm.CONFIG.franticMoveSpeed());
        }
    }
}
