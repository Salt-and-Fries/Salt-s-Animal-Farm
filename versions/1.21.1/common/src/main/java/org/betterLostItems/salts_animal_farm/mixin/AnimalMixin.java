package org.betterLostItems.salts_animal_farm.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;
import org.betterLostItems.salts_animal_farm.api.WeightedFarmAnimal;
import org.betterLostItems.salts_animal_farm.config.SaltsAnimalFarmConfig;
import org.betterLostItems.salts_animal_farm.entity.AnimalWeatherComfort;
import org.betterLostItems.salts_animal_farm.entity.SaltsAnimalFarmConfigLists;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Animal.class)
public abstract class AnimalMixin implements WeightedFarmAnimal {
    @Unique
    private static final String SALTS_ANIMAL_FARM_DATA = "SaltsAnimalFarm";
    @Unique
    private static final String WEIGHT = "Weight";
    @Unique
    private static final String SUCCESSFUL_TASK_STREAK = "SuccessfulTaskStreak";
    @Unique
    private static final String FAILED_TASK_STREAK = "FailedTaskStreak";
    @Unique
    private static final String TOTAL_SUCCESSFUL_TASKS = "TotalSuccessfulTasks";
    @Unique
    private static final String TOTAL_FAILED_TASKS = "TotalFailedTasks";
    @Unique
    private static final String LAST_COMFORT_TASK = "LastComfortTask";
    @Unique
    private static final String LAST_COMFORT_TASK_RESULT = "LastComfortTaskResult";
    @Unique
    private static final String FRANTIC_TICKS = "FranticTicks";
    @Unique
    private static final String SCARE_COOLDOWN_TICKS = "ScareCooldownTicks";
    @Unique
    private static final String RAIN_EXPOSURE_TICKS = "RainExposureTicks";
    @Unique
    private static final String CAN_BECOME_SICK = "CanBecomeSick";
    @Unique
    private static final ResourceLocation SICK_MOVEMENT_SPEED_ID = Salts_animal_farm.id("sick_movement_speed");

    @Unique
    private int salts_animal_farm$weight = 1;
    @Unique
    private int salts_animal_farm$successfulTaskStreak;
    @Unique
    private int salts_animal_farm$failedTaskStreak;
    @Unique
    private int salts_animal_farm$totalSuccessfulTasks;
    @Unique
    private int salts_animal_farm$totalFailedTasks;
    @Unique
    private String salts_animal_farm$currentComfortTask = "";
    @Unique
    private String salts_animal_farm$lastComfortTask = "Null";
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
    @Unique
    private boolean salts_animal_farm$canBecomeSick;

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void salts_animal_farm$saveWeightData(CompoundTag tag, CallbackInfo ci) {
        if (!Salts_animal_farm.CONFIG.modEnabled()) {
            return;
        }

        CompoundTag data = new CompoundTag();
        data.putInt(WEIGHT, salts_animal_farm$getWeight());
        data.putInt(SUCCESSFUL_TASK_STREAK, salts_animal_farm$getSuccessfulTaskStreak());
        data.putInt(FAILED_TASK_STREAK, salts_animal_farm$getFailedTaskStreak());
        data.putInt(TOTAL_SUCCESSFUL_TASKS, salts_animal_farm$getTotalSuccessfulTasks());
        data.putInt(TOTAL_FAILED_TASKS, salts_animal_farm$getTotalFailedTasks());
        data.putString(LAST_COMFORT_TASK, salts_animal_farm$getLastComfortTask());
        data.putString(LAST_COMFORT_TASK_RESULT, salts_animal_farm$getLastComfortTaskResult());
        data.putInt(FRANTIC_TICKS, salts_animal_farm$getFranticTicks());
        data.putInt(SCARE_COOLDOWN_TICKS, salts_animal_farm$getScareCooldownTicks());
        data.putInt(RAIN_EXPOSURE_TICKS, salts_animal_farm$getRainExposureTicks());
        data.putBoolean(CAN_BECOME_SICK, salts_animal_farm$canBecomeSick());
        tag.put(SALTS_ANIMAL_FARM_DATA, data);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void salts_animal_farm$readWeightData(CompoundTag tag, CallbackInfo ci) {
        if (!Salts_animal_farm.CONFIG.modEnabled()) {
            return;
        }

        CompoundTag data = tag.getCompound(SALTS_ANIMAL_FARM_DATA);
        salts_animal_farm$setCanBecomeSick(data.getBoolean(CAN_BECOME_SICK));
        salts_animal_farm$setWeight(data.getInt(WEIGHT));
        salts_animal_farm$setSuccessfulTaskStreak(data.getInt(SUCCESSFUL_TASK_STREAK));
        salts_animal_farm$setFailedTaskStreak(data.getInt(FAILED_TASK_STREAK));
        salts_animal_farm$setTotalSuccessfulTasks(data.getInt(TOTAL_SUCCESSFUL_TASKS));
        salts_animal_farm$setTotalFailedTasks(data.getInt(TOTAL_FAILED_TASKS));
        salts_animal_farm$setLastComfortTask(data.contains(LAST_COMFORT_TASK) ? data.getString(LAST_COMFORT_TASK) : "Null");
        salts_animal_farm$setLastComfortTaskResult(data.contains(LAST_COMFORT_TASK_RESULT) ? data.getString(LAST_COMFORT_TASK_RESULT) : "Null");
        salts_animal_farm$setCurrentComfortTask("");
        salts_animal_farm$setFranticTicks(data.getInt(FRANTIC_TICKS));
        salts_animal_farm$setScareCooldownTicks(data.getInt(SCARE_COOLDOWN_TICKS));
        salts_animal_farm$setRainExposureTicks(data.getInt(RAIN_EXPOSURE_TICKS));
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void salts_animal_farm$tickFarmAnimalData(CallbackInfo ci) {
        Animal animal = (Animal) (Object) this;

        if (!Salts_animal_farm.CONFIG.modEnabled() || !SaltsAnimalFarmConfigLists.isFarmAnimal(animal)) {
            salts_animal_farm$updateSickMovementSpeed(animal, false);
            return;
        }

        salts_animal_farm$setWeight(salts_animal_farm$getWeight());

        if (salts_animal_farm$franticTicks > 0) {
            salts_animal_farm$setFranticTicks(salts_animal_farm$franticTicks - 1);
        }

        if (salts_animal_farm$scareCooldownTicks > 0) {
            salts_animal_farm$setScareCooldownTicks(salts_animal_farm$scareCooldownTicks - 1);
        }

        salts_animal_farm$tickRainExposure(animal);
        salts_animal_farm$updateSickMovementSpeed(animal, salts_animal_farm$isSick());

        if (salts_animal_farm$isSick()) {
            animal.resetLove();
        }
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
        return salts_animal_farm$clampWeight(salts_animal_farm$weight);
    }

    @Override
    public void salts_animal_farm$setWeight(int weight) {
        Animal animal = (Animal) (Object) this;
        int clampedWeight = salts_animal_farm$clampWeight(weight);
        salts_animal_farm$weight = clampedWeight;
        salts_animal_farm$updateSickMovementSpeed(animal, SaltsAnimalFarmConfigLists.isFarmAnimal(animal) && clampedWeight <= 0);
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
    public int salts_animal_farm$getFailedTaskStreak() {
        return salts_animal_farm$failedTaskStreak;
    }

    @Override
    public void salts_animal_farm$setFailedTaskStreak(int streak) {
        salts_animal_farm$failedTaskStreak = Math.max(0, streak);
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
    public String salts_animal_farm$getLastComfortTask() {
        return salts_animal_farm$lastComfortTask == null ? "Null" : salts_animal_farm$lastComfortTask;
    }

    @Override
    public void salts_animal_farm$setLastComfortTask(String taskName) {
        String cleanTaskName = taskName == null || taskName.isBlank() ? "Null" : taskName;
        salts_animal_farm$lastComfortTask = cleanTaskName;
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

    @Override
    public boolean salts_animal_farm$canBecomeSick() {
        return salts_animal_farm$canBecomeSick;
    }

    @Override
    public void salts_animal_farm$setCanBecomeSick(boolean canBecomeSick) {
        salts_animal_farm$canBecomeSick = canBecomeSick;
        salts_animal_farm$setWeight(salts_animal_farm$getWeight());
    }

    @Override
    public SaltsAnimalFarmConfig.EffectiveValues salts_animal_farm$getEffectiveValues() {
        Animal animal = (Animal) (Object) this;
        return Salts_animal_farm.CONFIG.effectiveValues(animal.level());
    }

    @Inject(method = "canFallInLove", at = @At("HEAD"), cancellable = true)
    private void salts_animal_farm$preventSickLove(CallbackInfoReturnable<Boolean> cir) {
        Animal animal = (Animal) (Object) this;

        if (SaltsAnimalFarmConfigLists.isFarmAnimal(animal) && salts_animal_farm$isSick()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canMate", at = @At("HEAD"), cancellable = true)
    private void salts_animal_farm$preventSickMating(Animal other, CallbackInfoReturnable<Boolean> cir) {
        Animal animal = (Animal) (Object) this;

        if (SaltsAnimalFarmConfigLists.isFarmAnimal(animal) && salts_animal_farm$isSick()) {
            cir.setReturnValue(false);
            return;
        }

        if (other instanceof WeightedFarmAnimal otherWeightedAnimal
                && SaltsAnimalFarmConfigLists.isFarmAnimal(other)
                && otherWeightedAnimal.salts_animal_farm$isSick()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "finalizeSpawnChildFromBreeding", at = @At("TAIL"))
    private void salts_animal_farm$allowBredChildSickness(ServerLevel level, Animal otherParent, AgeableMob child, CallbackInfo ci) {
        if (child instanceof Animal childAnimal && child instanceof WeightedFarmAnimal weightedChild && SaltsAnimalFarmConfigLists.isFarmAnimal(childAnimal)) {
            weightedChild.salts_animal_farm$setCanBecomeSick(true);
            weightedChild.salts_animal_farm$setWeight(Math.max(1, weightedChild.salts_animal_farm$getEffectiveValues().minimumWeight()));
        }
    }

    @Unique
    private int salts_animal_farm$clampWeight(int weight) {
        Animal animal = (Animal) (Object) this;
        SaltsAnimalFarmConfig.EffectiveValues values = Salts_animal_farm.CONFIG.effectiveValues(animal.level());
        int minimumWeight = values.minimumWeight();

        if (minimumWeight <= 0 && !Salts_animal_farm.CONFIG.nonBredAnimalsCanBecomeSick() && !salts_animal_farm$canBecomeSick()) {
            minimumWeight = 1;
        }

        return Mth.clamp(weight, minimumWeight, values.maximumWeight());
    }

    @Unique
    private void salts_animal_farm$updateSickMovementSpeed(Animal animal, boolean sick) {
        AttributeInstance movementSpeed = animal.getAttribute(Attributes.MOVEMENT_SPEED);

        if (movementSpeed == null) {
            return;
        }

        if (!sick) {
            movementSpeed.removeModifier(SICK_MOVEMENT_SPEED_ID);
            return;
        }

        double multiplier = Salts_animal_farm.CONFIG.sanitizedSickMovementSpeedMultiplier();
        movementSpeed.addOrUpdateTransientModifier(new AttributeModifier(
                SICK_MOVEMENT_SPEED_ID,
                multiplier - 1.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));
    }
}
