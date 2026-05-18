package org.betterLostItems.salts_animal_farm.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;
import org.betterLostItems.salts_animal_farm.api.WeightedFarmAnimal;
import org.betterLostItems.salts_animal_farm.entity.AnimalWeatherComfort;
import org.betterLostItems.salts_animal_farm.entity.SaltsAnimalFarmConfigLists;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Animal.class)
public abstract class AnimalMixin implements WeightedFarmAnimal {
    @Unique
    private static final String SALTS_ANIMAL_FARM_DATA = "SaltsAnimalFarm";
    @Unique
    private static final String WEIGHT = "Weight";
    @Unique
    private static final String SUCCESSFUL_TASK_STREAK = "SuccessfulTaskStreak";
    @Unique
    private static final String TOTAL_SUCCESSFUL_TASKS = "TotalSuccessfulTasks";
    @Unique
    private static final String TOTAL_FAILED_TASKS = "TotalFailedTasks";
    @Unique
    private static final String LAST_COMFORT_TASK_RESULT = "LastComfortTaskResult";
    @Unique
    private static final String FRANTIC_TICKS = "FranticTicks";
    @Unique
    private static final String SCARE_COOLDOWN_TICKS = "ScareCooldownTicks";
    @Unique
    private static final String RAIN_EXPOSURE_TICKS = "RainExposureTicks";

    @Unique
    private int salts_animal_farm$weight;
    @Unique
    private int salts_animal_farm$successfulTaskStreak;
    @Unique
    private int salts_animal_farm$totalSuccessfulTasks;
    @Unique
    private int salts_animal_farm$totalFailedTasks;
    @Unique
    private String salts_animal_farm$currentComfortTask = "";
    @Unique
    private String salts_animal_farm$lastComfortTaskResult = "Null";
    @Unique
    private int salts_animal_farm$franticTicks;
    @Unique
    private int salts_animal_farm$scareCooldownTicks;
    @Unique
    private int salts_animal_farm$rainExposureTicks;
    @Unique
    private String salts_animal_farm$forcedComfortTask;

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void salts_animal_farm$saveWeightData(ValueOutput output, CallbackInfo ci) {
        ValueOutput data = output.child(SALTS_ANIMAL_FARM_DATA);
        data.putInt(WEIGHT, salts_animal_farm$getWeight());
        data.putInt(SUCCESSFUL_TASK_STREAK, salts_animal_farm$getSuccessfulTaskStreak());
        data.putInt(TOTAL_SUCCESSFUL_TASKS, salts_animal_farm$getTotalSuccessfulTasks());
        data.putInt(TOTAL_FAILED_TASKS, salts_animal_farm$getTotalFailedTasks());
        data.putString(LAST_COMFORT_TASK_RESULT, salts_animal_farm$getLastComfortTaskResult());
        data.putInt(FRANTIC_TICKS, salts_animal_farm$getFranticTicks());
        data.putInt(SCARE_COOLDOWN_TICKS, salts_animal_farm$getScareCooldownTicks());
        data.putInt(RAIN_EXPOSURE_TICKS, salts_animal_farm$getRainExposureTicks());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void salts_animal_farm$readWeightData(ValueInput input, CallbackInfo ci) {
        ValueInput data = input.childOrEmpty(SALTS_ANIMAL_FARM_DATA);
        salts_animal_farm$setWeight(data.getIntOr(WEIGHT, 0));
        salts_animal_farm$setSuccessfulTaskStreak(data.getIntOr(SUCCESSFUL_TASK_STREAK, 0));
        salts_animal_farm$setTotalSuccessfulTasks(data.getIntOr(TOTAL_SUCCESSFUL_TASKS, 0));
        salts_animal_farm$setTotalFailedTasks(data.getIntOr(TOTAL_FAILED_TASKS, 0));
        salts_animal_farm$setLastComfortTaskResult(data.getStringOr(LAST_COMFORT_TASK_RESULT, "Null"));
        salts_animal_farm$setCurrentComfortTask("");
        salts_animal_farm$setFranticTicks(data.getIntOr(FRANTIC_TICKS, 0));
        salts_animal_farm$setScareCooldownTicks(data.getIntOr(SCARE_COOLDOWN_TICKS, 0));
        salts_animal_farm$setRainExposureTicks(data.getIntOr(RAIN_EXPOSURE_TICKS, 0));
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void salts_animal_farm$tickFarmAnimalData(ServerLevel level, CallbackInfo ci) {
        Animal animal = (Animal) (Object) this;

        if (!SaltsAnimalFarmConfigLists.isFarmAnimal(animal)) {
            return;
        }

        if (salts_animal_farm$franticTicks > 0) {
            salts_animal_farm$setFranticTicks(salts_animal_farm$franticTicks - 1);
        }

        if (salts_animal_farm$scareCooldownTicks > 0) {
            salts_animal_farm$setScareCooldownTicks(salts_animal_farm$scareCooldownTicks - 1);
        }

        salts_animal_farm$tickRainExposure(animal);
    }

    @Unique
    private void salts_animal_farm$tickRainExposure(Animal animal) {
        if (!AnimalWeatherComfort.shouldSeekRainCover(animal)) {
            salts_animal_farm$setRainExposureTicks(0);
            return;
        }

        salts_animal_farm$setRainExposureTicks(salts_animal_farm$getRainExposureTicks() + 1);

        if (salts_animal_farm$getRainExposureTicks() >= AnimalWeatherComfort.RAIN_COVER_WEIGHT_LOSS_TICKS) {
            salts_animal_farm$addWeight(-1);
            salts_animal_farm$setRainExposureTicks(0);
        }
    }

    @Override
    public int salts_animal_farm$getWeight() {
        return salts_animal_farm$weight;
    }

    @Override
    public void salts_animal_farm$setWeight(int weight) {
        salts_animal_farm$weight = Mth.clamp(weight, Salts_animal_farm.CONFIG.minimumWeight(), Salts_animal_farm.CONFIG.maximumWeight());
    }

    @Override
    public int salts_animal_farm$getSuccessfulTaskStreak() {
        return salts_animal_farm$successfulTaskStreak;
    }

    @Override
    public void salts_animal_farm$setSuccessfulTaskStreak(int streak) {
        salts_animal_farm$successfulTaskStreak = Math.max(0, streak);
    }

    @Override
    public int salts_animal_farm$getTotalSuccessfulTasks() {
        return salts_animal_farm$totalSuccessfulTasks;
    }

    @Override
    public void salts_animal_farm$setTotalSuccessfulTasks(int total) {
        salts_animal_farm$totalSuccessfulTasks = Math.max(0, total);
    }

    @Override
    public int salts_animal_farm$getTotalFailedTasks() {
        return salts_animal_farm$totalFailedTasks;
    }

    @Override
    public void salts_animal_farm$setTotalFailedTasks(int total) {
        salts_animal_farm$totalFailedTasks = Math.max(0, total);
    }

    @Override
    public String salts_animal_farm$getCurrentComfortTask() {
        return salts_animal_farm$currentComfortTask;
    }

    @Override
    public void salts_animal_farm$setCurrentComfortTask(String taskName) {
        salts_animal_farm$currentComfortTask = taskName == null ? "" : taskName;
    }

    @Override
    public String salts_animal_farm$getLastComfortTaskResult() {
        return salts_animal_farm$lastComfortTaskResult == null ? "Null" : salts_animal_farm$lastComfortTaskResult;
    }

    @Override
    public void salts_animal_farm$setLastComfortTaskResult(String result) {
        String cleanResult = result == null || result.isBlank() ? "Null" : result;
        salts_animal_farm$lastComfortTaskResult = cleanResult;
    }

    @Override
    public int salts_animal_farm$getFranticTicks() {
        return salts_animal_farm$franticTicks;
    }

    @Override
    public void salts_animal_farm$setFranticTicks(int ticks) {
        salts_animal_farm$franticTicks = Math.max(0, ticks);
    }

    @Override
    public int salts_animal_farm$getScareCooldownTicks() {
        return salts_animal_farm$scareCooldownTicks;
    }

    @Override
    public void salts_animal_farm$setScareCooldownTicks(int ticks) {
        salts_animal_farm$scareCooldownTicks = Math.max(0, ticks);
    }

    @Override
    public int salts_animal_farm$getRainExposureTicks() {
        return salts_animal_farm$rainExposureTicks;
    }

    @Override
    public void salts_animal_farm$setRainExposureTicks(int ticks) {
        salts_animal_farm$rainExposureTicks = Math.max(0, ticks);
    }

    @Override
    public void salts_animal_farm$forceComfortTask(String taskName) {
        salts_animal_farm$forcedComfortTask = taskName == null ? "" : taskName;
    }

    @Override
    public String salts_animal_farm$consumeForcedComfortTask() {
        String taskName = salts_animal_farm$forcedComfortTask;
        salts_animal_farm$forcedComfortTask = null;
        return taskName;
    }
}
