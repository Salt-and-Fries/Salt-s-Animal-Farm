package org.betterLostItems.salts_animal_farm.api;

import org.betterLostItems.salts_animal_farm.config.SaltsAnimalFarmConfig;

public interface WeightedFarmAnimal {
    int salts_animal_farm$getWeight();

    void salts_animal_farm$setWeight(int weight);

    int salts_animal_farm$getSuccessfulTaskStreak();

    void salts_animal_farm$setSuccessfulTaskStreak(int streak);

    int salts_animal_farm$getFailedTaskStreak();

    void salts_animal_farm$setFailedTaskStreak(int streak);

    int salts_animal_farm$getTotalSuccessfulTasks();

    void salts_animal_farm$setTotalSuccessfulTasks(int total);

    int salts_animal_farm$getTotalFailedTasks();

    void salts_animal_farm$setTotalFailedTasks(int total);

    String salts_animal_farm$getCurrentComfortTask();

    void salts_animal_farm$setCurrentComfortTask(String taskName);

    String salts_animal_farm$getLastComfortTaskResult();

    void salts_animal_farm$setLastComfortTaskResult(String result);

    String salts_animal_farm$getLastComfortTask();

    void salts_animal_farm$setLastComfortTask(String taskName);

    int salts_animal_farm$getFranticTicks();

    void salts_animal_farm$setFranticTicks(int ticks);

    int salts_animal_farm$getScareCooldownTicks();

    void salts_animal_farm$setScareCooldownTicks(int ticks);

    int salts_animal_farm$getRainExposureTicks();

    void salts_animal_farm$setRainExposureTicks(int ticks);

    void salts_animal_farm$forceComfortTask(String taskName);

    String salts_animal_farm$consumeForcedComfortTask();

    boolean salts_animal_farm$canBecomeSick();

    void salts_animal_farm$setCanBecomeSick(boolean canBecomeSick);

    SaltsAnimalFarmConfig.EffectiveValues salts_animal_farm$getEffectiveValues();

    default void salts_animal_farm$addWeight(int amount) {
        salts_animal_farm$setWeight(salts_animal_farm$getWeight() + amount);
    }

    default void salts_animal_farm$recordComfortSuccess() {
        int nextStreak = salts_animal_farm$getSuccessfulTaskStreak() + 1;
        SaltsAnimalFarmConfig.EffectiveValues values = salts_animal_farm$getEffectiveValues();
        salts_animal_farm$setSuccessfulTaskStreak(nextStreak);
        salts_animal_farm$setFailedTaskStreak(0);
        salts_animal_farm$setTotalSuccessfulTasks(salts_animal_farm$getTotalSuccessfulTasks() + 1);
        salts_animal_farm$setLastComfortTask(salts_animal_farm$getCurrentComfortTask());
        salts_animal_farm$setLastComfortTaskResult("Success");
        salts_animal_farm$setCurrentComfortTask("");

        if (nextStreak >= values.positiveTaskStreakThreshold()) {
            salts_animal_farm$addWeight(1);
        }
    }

    default void salts_animal_farm$recordComfortFailure() {
        int nextStreak = salts_animal_farm$getFailedTaskStreak() + 1;
        SaltsAnimalFarmConfig.EffectiveValues values = salts_animal_farm$getEffectiveValues();
        salts_animal_farm$setFailedTaskStreak(nextStreak);
        salts_animal_farm$setSuccessfulTaskStreak(0);
        salts_animal_farm$setTotalFailedTasks(salts_animal_farm$getTotalFailedTasks() + 1);
        salts_animal_farm$setLastComfortTask(salts_animal_farm$getCurrentComfortTask());
        salts_animal_farm$setLastComfortTaskResult("Failed");
        salts_animal_farm$setCurrentComfortTask("");

        if (nextStreak >= values.negativeTaskStreakThreshold()) {
            salts_animal_farm$addWeight(-1);
        }
    }

    default void salts_animal_farm$resetComfortStreak() {
        salts_animal_farm$setSuccessfulTaskStreak(0);
        salts_animal_farm$setFailedTaskStreak(0);
    }

    default boolean salts_animal_farm$isSick() {
        return salts_animal_farm$getWeight() <= 0;
    }

    default boolean salts_animal_farm$isFrantic() {
        return salts_animal_farm$getFranticTicks() > 0;
    }

    default void salts_animal_farm$startFrantic(int ticks) {
        if (ticks > salts_animal_farm$getFranticTicks()) {
            salts_animal_farm$setFranticTicks(ticks);
        }
    }

    default boolean salts_animal_farm$tryHostileScare(int cooldownTicks) {
        if (salts_animal_farm$getScareCooldownTicks() > 0) {
            return false;
        }

        salts_animal_farm$addWeight(-1);
        salts_animal_farm$resetComfortStreak();
        salts_animal_farm$setScareCooldownTicks(cooldownTicks);
        return true;
    }
}
