package org.betterLostItems.salts_animal_farm.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;
import org.betterLostItems.salts_animal_farm.api.WeightedFarmAnimal;
import org.betterLostItems.salts_animal_farm.config.SaltsAnimalFarmConfig;
import org.betterLostItems.salts_animal_farm.entity.AnimalWeatherComfort;
import org.betterLostItems.salts_animal_farm.entity.SaltsAnimalFarmConfigLists;
import org.betterLostItems.salts_animal_farm.mixin.LivingEntitySoundAccessor;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;

public class AnimalComfortGoal extends Goal {
    private static final double FRIEND_COMPLETION_DISTANCE_SQR = 16.0D;
    private static final double FRIEND_RETARGET_DISTANCE_SQR = 4.0D;
    private static final double SPACE_CLEAR_RADIUS = 1.5D;
    private static final double SPACE_MOB_CLEAR_RADIUS = 1.5D;
    private static final int SPACE_RETARGET_INTERVAL_TICKS = 10;
    private static final int COVER_TARGET_DEEPEN_RADIUS = 2;

    private static final List<ComfortTask> DAY_TASKS = List.of(
            ComfortTask.SHADE,
            ComfortTask.SUNLIGHT,
            ComfortTask.WATER,
            ComfortTask.SPACE,
            ComfortTask.FRIEND
    );
    private static final List<ComfortTask> DAY_RAIN_TASKS = List.of(
            ComfortTask.SHADE,
            ComfortTask.SUNLIGHT,
            ComfortTask.WATER,
            ComfortTask.FRIEND
    );
    private static final List<ComfortTask> NIGHT_TASKS = List.of(
            ComfortTask.LIGHT,
            ComfortTask.NAP,
            ComfortTask.STARS,
            ComfortTask.WATER,
            ComfortTask.SPACE,
            ComfortTask.FRIEND
    );
    private static final List<ComfortTask> NIGHT_RAIN_TASKS = List.of(
            ComfortTask.LIGHT,
            ComfortTask.NAP,
            ComfortTask.STARS,
            ComfortTask.WATER,
            ComfortTask.FRIEND
    );

    private final Animal animal;
    private final WeightedFarmAnimal weightedAnimal;
    private ComfortTask activeTask;
    private BlockPos targetPos;
    private Animal targetFriend;
    private int nextAttemptTick;
    private int nextRainCoverAttemptTick;
    private int activeTicks;
    private int lingerTicks;

    public AnimalComfortGoal(Animal animal) {
        this.animal = animal;
        this.weightedAnimal = (WeightedFarmAnimal) animal;
        setFlags(EnumSet.of(Flag.MOVE));
        scheduleNextAttempt();
    }

    @Override
    public boolean canUse() {
        String forcedTaskName = weightedAnimal.salts_animal_farm$consumeForcedComfortTask();
        boolean forced = forcedTaskName != null;

        if (forced) {
            debug("forced task request rawTask='" + forcedTaskName + "'");
        }

        if (!SaltsAnimalFarmConfigLists.isFarmAnimal(animal)) {
            if (forced) {
                debug("canUse=false reason=not_configured_farm_animal");
            }
            return false;
        }

        if (weightedAnimal.salts_animal_farm$isFrantic()) {
            if (forced) {
                debug("canUse=false reason=animal_frantic franticTicks=" + weightedAnimal.salts_animal_farm$getFranticTicks());
            }
            return false;
        }

        boolean rainCoverNeeded = !forced && AnimalWeatherComfort.shouldSeekRainCover(animal);

        if (rainCoverNeeded && animal.tickCount < nextRainCoverAttemptTick) {
            return false;
        }

        if (!forced && !rainCoverNeeded && animal.tickCount < nextAttemptTick) {
            return false;
        }

        if (rainCoverNeeded) {
            debug("rain cover task attempt is urgent rainExposureTicks=" + weightedAnimal.salts_animal_farm$getRainExposureTicks());
        } else if (!forced) {
            debug("random scheduled task attempt is due nextAttemptTick=" + nextAttemptTick);
            scheduleNextAttempt();
        }

        ComfortTask task;
        if (rainCoverNeeded) {
            task = ComfortTask.COVER;
        } else if (forcedTaskName == null || forcedTaskName.isBlank()) {
            task = pickTask();
        } else {
            task = ComfortTask.byName(forcedTaskName);
        }

        if (task == null) {
            debug("canUse=false reason=unknown_task rawTask='" + forcedTaskName + "' -> recording failure");
            recordTaskFailure();
            return false;
        }

        activeTask = task;
        targetFriend = null;
        weightedAnimal.salts_animal_farm$setCurrentComfortTask(task.taskName());
        BlockPos currentPos = animal.blockPosition();
        debug(() -> "selected task=" + task.taskName() + " forced=" + forced + " currentPos=" + posString(currentPos) + " currentCheck={" + taskCheckDetails(task, currentPos) + "}");

        if (isCompletionSpot(task, currentPos)) {
            targetPos = currentPos;
            debug("task already satisfied at current position, starting linger currentPos=" + posString(currentPos));
            return true;
        }

        targetPos = findTarget(task);

        if (targetPos == null) {
            if (task == ComfortTask.COVER) {
                debug(() -> "canUse=false reason=no_cover_found task=" + task.taskName() + " rainExposureTicks=" + weightedAnimal.salts_animal_farm$getRainExposureTicks() + " currentCheck={" + taskCheckDetails(task, currentPos) + "}");
                scheduleNextRainCoverAttempt();
                activeTask = null;
                weightedAnimal.salts_animal_farm$setCurrentComfortTask("");
            } else {
                debug(() -> "canUse=false reason=no_target_found task=" + task.taskName() + " -> recording failure currentCheck={" + taskCheckDetails(task, currentPos) + "}");
                recordTaskFailure();
                activeTask = null;
            }
            return false;
        }

        debug(() -> "canUse=true task=" + task.taskName() + " target=" + posString(targetPos) + " targetCheck={" + taskCheckDetails(task, targetPos) + "}");
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        int maxTaskTicks = taskTimeoutTicks();
        if (targetPos == null || activeTicks >= maxTaskTicks) {
            if (activeTask != null) {
                debug(() -> "canContinue=false task=" + activeTask.taskName()
                        + " target=" + posString(targetPos)
                        + " targetNull=" + (targetPos == null)
                        + " dangerInterrupt=" + hasDangerInterrupt()
                        + " frantic=" + weightedAnimal.salts_animal_farm$isFrantic()
                        + " hurtTime=" + animal.hurtTime
                        + " activeTicks=" + activeTicks
                        + " maxTicks=" + Salts_animal_farm.CONFIG.comfortMaxTaskTicks()
                        + " reachTimeoutTicks=" + Salts_animal_farm.CONFIG.comfortTaskReachTimeoutTicks());
            }
            return false;
        }

        boolean dangerInterrupt = hasDangerInterrupt();

        if (dangerInterrupt && activeTask != null) {
            debug("canContinue=false task=" + activeTask.taskName()
                    + " target=" + posString(targetPos)
                    + " targetNull=false"
                    + " dangerInterrupt=" + dangerInterrupt
                    + " frantic=" + weightedAnimal.salts_animal_farm$isFrantic()
                    + " hurtTime=" + animal.hurtTime
                    + " activeTicks=" + activeTicks
                    + " maxTicks=" + Salts_animal_farm.CONFIG.comfortMaxTaskTicks()
                    + " reachTimeoutTicks=" + Salts_animal_farm.CONFIG.comfortTaskReachTimeoutTicks());
        }

        return !dangerInterrupt;
    }

    @Override
    public boolean isInterruptable() {
        return hasDangerInterrupt();
    }

    @Override
    public void start() {
        activeTicks = 0;
        lingerTicks = 0;
        animal.getNavigation().moveTo(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D, Salts_animal_farm.CONFIG.comfortMoveSpeed());
        debug("start task=" + taskNameOrNull()
                + " target=" + posString(targetPos)
                + " speed=" + Salts_animal_farm.CONFIG.comfortMoveSpeed()
                + " navDoneAfterMove=" + animal.getNavigation().isDone());
    }

    @Override
    public void stop() {
        if (targetPos != null && activeTask != null) {
            BlockPos currentPos = animal.blockPosition();
            double targetDistanceSqr = distanceToCenterSqr(targetPos);
            boolean currentCompletion = isCompletionSpot(activeTask, currentPos);
            boolean targetCompletion = canCompleteFromTarget(activeTask) && targetDistanceSqr <= 4.0D && isCompletionSpot(activeTask, targetPos);
            boolean dangerInterrupt = hasDangerInterrupt();
            boolean timeout = activeTicks >= taskTimeoutTicks();

            if (currentCompletion || targetCompletion) {
                if (lingerTicks > 0) {
                    debug(() -> "stop interrupted task while condition is satisfied -> recording success task=" + activeTask.taskName()
                            + " target=" + posString(targetPos)
                            + " activeTicks=" + activeTicks
                            + " lingerTicks=" + lingerTicks
                            + " currentPos=" + posString(currentPos)
                            + " distSqr=" + String.format(java.util.Locale.ROOT, "%.3f", targetDistanceSqr)
                            + " currentCompletion=" + currentCompletion
                            + " targetCompletion=" + targetCompletion
                            + " currentCheck={" + taskCheckDetails(activeTask, currentPos) + "}"
                            + " targetCheck={" + taskCheckDetails(activeTask, targetPos) + "}");
                    if (activeTask == ComfortTask.COVER) {
                        recordNeutralTaskCompletion("interrupted_while_satisfied");
                    } else {
                        recordTaskSuccess("interrupted_while_satisfied");
                    }
                } else {
                    debug(() -> "stop interrupted task before linger but condition is satisfied -> neutral stop task=" + activeTask.taskName()
                            + " target=" + posString(targetPos)
                            + " activeTicks=" + activeTicks
                            + " lingerTicks=" + lingerTicks
                            + " currentPos=" + posString(currentPos)
                            + " distSqr=" + String.format(java.util.Locale.ROOT, "%.3f", targetDistanceSqr)
                            + " currentCompletion=" + currentCompletion
                            + " targetCompletion=" + targetCompletion
                            + " currentCheck={" + taskCheckDetails(activeTask, currentPos) + "}"
                            + " targetCheck={" + taskCheckDetails(activeTask, targetPos) + "}");
                }
            } else if (dangerInterrupt) {
                debug(() -> "stop interrupted by danger -> neutral stop task=" + activeTask.taskName()
                        + " target=" + posString(targetPos)
                        + " activeTicks=" + activeTicks
                        + " lingerTicks=" + lingerTicks
                        + " currentPos=" + posString(currentPos)
                        + " distSqr=" + String.format(java.util.Locale.ROOT, "%.3f", targetDistanceSqr)
                        + " currentCompletion=" + currentCompletion
                        + " targetCompletion=" + targetCompletion
                        + " frantic=" + weightedAnimal.salts_animal_farm$isFrantic()
                        + " hurtTime=" + animal.hurtTime
                        + " currentCheck={" + taskCheckDetails(activeTask, currentPos) + "}"
                        + " targetCheck={" + taskCheckDetails(activeTask, targetPos) + "}");
            } else if (!timeout) {
                debug(() -> "stop interrupted unfinished task before timeout -> neutral stop task=" + activeTask.taskName()
                        + " target=" + posString(targetPos)
                        + " activeTicks=" + activeTicks
                        + " lingerTicks=" + lingerTicks
                        + " currentPos=" + posString(currentPos)
                        + " distSqr=" + String.format(java.util.Locale.ROOT, "%.3f", targetDistanceSqr)
                        + " currentCompletion=" + currentCompletion
                        + " targetCompletion=" + targetCompletion
                        + " currentCheck={" + taskCheckDetails(activeTask, currentPos) + "}"
                        + " targetCheck={" + taskCheckDetails(activeTask, targetPos) + "}");
            } else if (activeTask == ComfortTask.COVER) {
                debug(() -> "stop unfinished cover task after timeout -> neutral stop target=" + posString(targetPos)
                        + " activeTicks=" + activeTicks
                        + " lingerTicks=" + lingerTicks
                        + " rainExposureTicks=" + weightedAnimal.salts_animal_farm$getRainExposureTicks()
                        + " currentPos=" + posString(currentPos)
                        + " distSqr=" + String.format(java.util.Locale.ROOT, "%.3f", targetDistanceSqr)
                        + " currentCompletion=" + currentCompletion
                        + " targetCompletion=" + targetCompletion
                        + " currentCheck={" + taskCheckDetails(activeTask, currentPos) + "}"
                        + " targetCheck={" + taskCheckDetails(activeTask, targetPos) + "}");
            } else {
                debug(() -> "stop with unfinished task after timeout -> recording failure task=" + activeTask.taskName()
                        + " target=" + posString(targetPos)
                        + " activeTicks=" + activeTicks
                        + " lingerTicks=" + lingerTicks
                        + " currentPos=" + posString(currentPos)
                        + " distSqr=" + String.format(java.util.Locale.ROOT, "%.3f", targetDistanceSqr)
                        + " currentCompletion=" + currentCompletion
                        + " targetCompletion=" + targetCompletion
                        + " currentCheck={" + taskCheckDetails(activeTask, currentPos) + "}"
                        + " targetCheck={" + taskCheckDetails(activeTask, targetPos) + "}");
                recordTaskFailure();
            }
        } else {
            debug("stop without failure activeTask=" + taskNameOrNull()
                    + " target=" + posString(targetPos)
                    + " activeTicks=" + activeTicks
                    + " lingerTicks=" + lingerTicks);
        }

        activeTask = null;
        weightedAnimal.salts_animal_farm$setCurrentComfortTask("");
        targetPos = null;
        targetFriend = null;
        activeTicks = 0;
        lingerTicks = 0;
        animal.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetPos == null) {
            return;
        }

        activeTicks++;
        updateFriendTarget();
        updateSpaceTarget();
        BlockPos currentPos = animal.blockPosition();
        double targetDistanceSqr = distanceToCenterSqr(targetPos);
        boolean currentCompletion = activeTask != null && isCompletionSpot(activeTask, currentPos);
        boolean targetCompletion = activeTask != null && canCompleteFromTarget(activeTask) && targetDistanceSqr <= 4.0D && isCompletionSpot(activeTask, targetPos);
        boolean navigationDone = animal.getNavigation().isDone();

        if (debugEnabled() && (activeTicks <= 5 || activeTicks % 20 == 0 || currentCompletion || targetCompletion || navigationDone)) {
            debug("tick task=" + taskNameOrNull()
                    + " activeTicks=" + activeTicks
                    + " lingerTicks=" + lingerTicks
                    + " currentPos=" + posString(currentPos)
                    + " target=" + posString(targetPos)
                    + " distSqr=" + String.format(java.util.Locale.ROOT, "%.3f", targetDistanceSqr)
                    + " navDone=" + navigationDone
                    + " currentCompletion=" + currentCompletion
                    + " targetCompletion=" + targetCompletion
                    + " currentCheck={" + (activeTask == null ? "no active task" : taskCheckDetails(activeTask, currentPos)) + "}"
                    + " targetCheck={" + (activeTask == null ? "no active task" : taskCheckDetails(activeTask, targetPos)) + "}");
        }

        if (activeTask != null && (currentCompletion || targetCompletion)) {
            animal.getNavigation().stop();
            lingerTicks++;
            debug(() -> "completion condition reached during path -> completing immediately task=" + activeTask.taskName()
                    + " currentCompletion=" + currentCompletion
                    + " targetCompletion=" + targetCompletion
                    + " currentPos=" + posString(currentPos)
                    + " target=" + posString(targetPos)
                    + " distSqr=" + String.format(java.util.Locale.ROOT, "%.3f", targetDistanceSqr)
                    + " currentCheck={" + taskCheckDetails(activeTask, currentPos) + "}"
                    + " targetCheck={" + taskCheckDetails(activeTask, targetPos) + "}");
            completeTask("condition_reached");
            return;
        }

        if (navigationDone) {
            if (activeTask == ComfortTask.COVER) {
                BlockPos betterTarget = findCoverTarget();
                if (betterTarget != null && !betterTarget.equals(targetPos)) {
                    debug(() -> "cover navigation ended exposed, retargeting deeper cover oldTarget=" + posString(targetPos)
                            + " newTarget=" + posString(betterTarget)
                            + " currentPos=" + posString(currentPos)
                            + " currentCheck={" + taskCheckDetails(activeTask, currentPos) + "}");
                    targetPos = betterTarget;
                    animal.getNavigation().moveTo(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D, Salts_animal_farm.CONFIG.comfortMoveSpeed());
                    return;
                }
            }

            debug("navigation done before completion, repathing to target=" + posString(targetPos));
            animal.getNavigation().moveTo(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D, Salts_animal_farm.CONFIG.comfortMoveSpeed());
        }
    }

    public ComfortTask getActiveTask() {
        return activeTask;
    }

    private ComfortTask pickTask() {
        boolean daytime = isDaytime();
        List<ComfortTask> tasks;

        if (AnimalWeatherComfort.isRainingInRainBiome(animal)) {
            tasks = daytime ? DAY_RAIN_TASKS : NIGHT_RAIN_TASKS;
        } else {
            tasks = daytime ? DAY_TASKS : NIGHT_TASKS;
        }

        return tasks.get(animal.getRandom().nextInt(tasks.size()));
    }

    private int taskTimeoutTicks() {
        SaltsAnimalFarmConfig config = Salts_animal_farm.CONFIG;
        return Math.min(config.comfortMaxTaskTicks(), config.comfortTaskReachTimeoutTicks());
    }

    private boolean isDaytime() {
        long timeOfDay = animal.level().getOverworldClockTime() % 24000L;
        return timeOfDay >= 0L && timeOfDay < 12000L;
    }

    private BlockPos findTarget(ComfortTask task) {
        if (task == ComfortTask.FRIEND) {
            return findFriendTarget();
        }

        if (task == ComfortTask.COVER) {
            return findCoverTarget();
        }

        if (task == ComfortTask.SPACE) {
            return findSpaceTarget();
        }

        SaltsAnimalFarmConfig config = Salts_animal_farm.CONFIG;
        RandomSource random = animal.getRandom();
        BlockPos origin = animal.blockPosition();
        int checked = 0;
        int validStandingSpots = 0;

        debug("findTarget start task=" + task.taskName()
                + " origin=" + posString(origin)
                + " searchRadius=" + config.comfortSearchRadius()
                + " verticalSearch=" + config.comfortVerticalSearch()
                + " randomSamples=" + config.comfortSearchSamples());
        for (int radius = 1; radius <= config.comfortSearchRadius(); radius++) {
            for (BlockPos candidate : BlockPos.betweenClosed(
                    origin.offset(-radius, -config.comfortVerticalSearch(), -radius),
                    origin.offset(radius, config.comfortVerticalSearch(), radius))) {
                if (Math.abs(candidate.getX() - origin.getX()) != radius && Math.abs(candidate.getZ() - origin.getZ()) != radius) {
                    continue;
                }

                BlockPos immutableCandidate = candidate.immutable();
                checked++;
                boolean validStandingSpot = isValidStandingSpot(immutableCandidate);
                boolean matchesTask = validStandingSpot && matchesTask(task, immutableCandidate);

                if (validStandingSpot) {
                    validStandingSpots++;
                }

                debugCandidate("deterministic", task, immutableCandidate, checked, validStandingSpot, matchesTask);

                if (validStandingSpot && matchesTask && canNavigateTo(immutableCandidate)) {
                    int loggedChecked = checked;
                    int loggedValidStandingSpots = validStandingSpots;
                    debug(() -> "findTarget success phase=deterministic task=" + task.taskName()
                            + " target=" + posString(immutableCandidate)
                            + " checked=" + loggedChecked
                            + " validStandingSpots=" + loggedValidStandingSpots
                            + " details={" + taskCheckDetails(task, immutableCandidate) + "}");
                    return immutableCandidate;
                } else if (validStandingSpot && matchesTask) {
                    debug("candidate rejected phase=deterministic task=" + task.taskName()
                            + " pos=" + posString(immutableCandidate)
                            + " reason=no_path");
                }
            }
        }

        for (int i = 0; i < config.comfortSearchSamples(); i++) {
            BlockPos candidate = origin.offset(
                    random.nextInt(config.comfortSearchRadius() * 2 + 1) - config.comfortSearchRadius(),
                    random.nextInt(config.comfortVerticalSearch() * 2 + 1) - config.comfortVerticalSearch(),
                    random.nextInt(config.comfortSearchRadius() * 2 + 1) - config.comfortSearchRadius()
            );
            checked++;
            boolean validStandingSpot = isValidStandingSpot(candidate);
            boolean matchesTask = validStandingSpot && matchesTask(task, candidate);

            if (validStandingSpot) {
                validStandingSpots++;
            }

            debugCandidate("random", task, candidate, checked, validStandingSpot, matchesTask);

            if (validStandingSpot && matchesTask && canNavigateTo(candidate)) {
                int loggedChecked = checked;
                int loggedValidStandingSpots = validStandingSpots;
                debug(() -> "findTarget success phase=random task=" + task.taskName()
                        + " target=" + posString(candidate)
                        + " checked=" + loggedChecked
                        + " validStandingSpots=" + loggedValidStandingSpots
                        + " details={" + taskCheckDetails(task, candidate) + "}");
                return candidate;
            } else if (validStandingSpot && matchesTask) {
                debug("candidate rejected phase=random task=" + task.taskName()
                        + " pos=" + posString(candidate)
                        + " reason=no_path");
            }
        }

        debug("findTarget failed task=" + task.taskName()
                + " origin=" + posString(origin)
                + " checked=" + checked
                + " validStandingSpots=" + validStandingSpots);
        return null;
    }

    private BlockPos findCoverTarget() {
        SaltsAnimalFarmConfig config = Salts_animal_farm.CONFIG;
        RandomSource random = animal.getRandom();
        BlockPos origin = animal.blockPosition();
        CoverCandidate best = null;
        int checked = 0;
        int validStandingSpots = 0;

        debug("findCoverTarget start origin=" + posString(origin)
                + " searchRadius=" + config.comfortSearchRadius()
                + " verticalSearch=" + config.comfortVerticalSearch()
                + " randomSamples=" + config.comfortSearchSamples());

        for (int radius = 1; radius <= config.comfortSearchRadius(); radius++) {
            for (BlockPos candidate : BlockPos.betweenClosed(
                    origin.offset(-radius, -config.comfortVerticalSearch(), -radius),
                    origin.offset(radius, config.comfortVerticalSearch(), radius))) {
                if (Math.abs(candidate.getX() - origin.getX()) != radius && Math.abs(candidate.getZ() - origin.getZ()) != radius) {
                    continue;
                }

                BlockPos immutableCandidate = candidate.immutable();
                checked++;
                boolean validStandingSpot = isValidStandingSpot(immutableCandidate);
                boolean matchesTask = validStandingSpot && matchesTask(ComfortTask.COVER, immutableCandidate);

                if (validStandingSpot) {
                    validStandingSpots++;
                }

                debugCandidate("deterministic", ComfortTask.COVER, immutableCandidate, checked, validStandingSpot, matchesTask);
                if (matchesTask) {
                    best = chooseBetterCoverCandidate(best, evaluateCoverCandidate(origin, immutableCandidate, "deterministic"));
                }
            }
        }

        for (int i = 0; i < config.comfortSearchSamples(); i++) {
            BlockPos candidate = origin.offset(
                    random.nextInt(config.comfortSearchRadius() * 2 + 1) - config.comfortSearchRadius(),
                    random.nextInt(config.comfortVerticalSearch() * 2 + 1) - config.comfortVerticalSearch(),
                    random.nextInt(config.comfortSearchRadius() * 2 + 1) - config.comfortSearchRadius()
            ).immutable();
            checked++;
            boolean validStandingSpot = isValidStandingSpot(candidate);
            boolean matchesTask = validStandingSpot && matchesTask(ComfortTask.COVER, candidate);

            if (validStandingSpot) {
                validStandingSpots++;
            }

            debugCandidate("random", ComfortTask.COVER, candidate, checked, validStandingSpot, matchesTask);
            if (matchesTask) {
                best = chooseBetterCoverCandidate(best, evaluateCoverCandidate(origin, candidate, "random"));
            }
        }

        if (best != null) {
            CoverCandidate loggedBest = best;
            int loggedChecked = checked;
            int loggedValidStandingSpots = validStandingSpots;
            debug(() -> "findCoverTarget success target=" + posString(loggedBest.pos())
                    + " score=" + loggedBest.score()
                    + " checked=" + loggedChecked
                    + " validStandingSpots=" + loggedValidStandingSpots
                    + " details={" + taskCheckDetails(ComfortTask.COVER, loggedBest.pos()) + "}");
            return loggedBest.pos();
        }

        debug("findCoverTarget failed origin=" + posString(origin)
                + " checked=" + checked
                + " validStandingSpots=" + validStandingSpots);
        return null;
    }

    private CoverCandidate evaluateCoverCandidate(BlockPos origin, BlockPos candidate, String phase) {
        CoverCandidate deepest = findDeepestNearbyCover(origin, candidate);
        if (deepest == null) {
            debug("candidate rejected phase=" + phase
                    + " task=cover pos=" + posString(candidate)
                    + " reason=no_deeper_reachable_cover");
            return null;
        }

        return deepest;
    }

    private CoverCandidate findDeepestNearbyCover(BlockPos origin, BlockPos seed) {
        CoverCandidate best = isPathExact(createPathTo(seed), seed) ? new CoverCandidate(seed, coverScore(origin, seed)) : null;

        for (int dx = -COVER_TARGET_DEEPEN_RADIUS; dx <= COVER_TARGET_DEEPEN_RADIUS; dx++) {
            for (int dz = -COVER_TARGET_DEEPEN_RADIUS; dz <= COVER_TARGET_DEEPEN_RADIUS; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                BlockPos candidate = seed.offset(dx, 0, dz);
                if (!isValidStandingSpot(candidate) || !matchesTask(ComfortTask.COVER, candidate)) {
                    continue;
                }

                Path path = createPathTo(candidate);
                if (!isPathExact(path, candidate)) {
                    continue;
                }

                best = chooseBetterCoverCandidate(best, new CoverCandidate(candidate, coverScore(origin, candidate)));
            }
        }

        return best;
    }

    private CoverCandidate chooseBetterCoverCandidate(CoverCandidate current, CoverCandidate candidate) {
        if (candidate == null) {
            return current;
        }

        if (current == null || candidate.score() > current.score()) {
            return candidate;
        }

        return current;
    }

    private BlockPos findSpaceTarget() {
        SaltsAnimalFarmConfig config = Salts_animal_farm.CONFIG;
        RandomSource random = animal.getRandom();
        BlockPos origin = animal.blockPosition();
        Vec3 crowdCenter = nearbyAnimalCenter(config.comfortSearchRadius());
        SpaceCandidate best = null;
        int checked = 0;
        int validStandingSpots = 0;

        debug("findSpaceTarget start origin=" + posString(origin)
                + " crowdCenter=" + vecString(crowdCenter)
                + " searchRadius=" + config.comfortSearchRadius()
                + " verticalSearch=" + config.comfortVerticalSearch()
                + " randomSamples=" + config.comfortSearchSamples());

        for (int radius = 1; radius <= config.comfortSearchRadius(); radius++) {
            for (BlockPos candidate : BlockPos.betweenClosed(
                    origin.offset(-radius, -config.comfortVerticalSearch(), -radius),
                    origin.offset(radius, config.comfortVerticalSearch(), radius))) {
                if (Math.abs(candidate.getX() - origin.getX()) != radius && Math.abs(candidate.getZ() - origin.getZ()) != radius) {
                    continue;
                }

                BlockPos immutableCandidate = candidate.immutable();
                checked++;
                boolean validStandingSpot = isValidStandingSpot(immutableCandidate);
                boolean matchesTask = validStandingSpot && matchesTask(ComfortTask.SPACE, immutableCandidate);

                if (validStandingSpot) {
                    validStandingSpots++;
                }

                debugCandidate("deterministic", ComfortTask.SPACE, immutableCandidate, checked, validStandingSpot, matchesTask);

                if (matchesTask) {
                    SpaceCandidate spaceCandidate = evaluateSpaceCandidate(origin, immutableCandidate, crowdCenter, "deterministic");
                    best = chooseBetterSpaceCandidate(best, spaceCandidate);
                }
            }
        }

        for (int i = 0; i < config.comfortSearchSamples(); i++) {
            BlockPos candidate = origin.offset(
                    random.nextInt(config.comfortSearchRadius() * 2 + 1) - config.comfortSearchRadius(),
                    random.nextInt(config.comfortVerticalSearch() * 2 + 1) - config.comfortVerticalSearch(),
                    random.nextInt(config.comfortSearchRadius() * 2 + 1) - config.comfortSearchRadius()
            ).immutable();
            checked++;
            boolean validStandingSpot = isValidStandingSpot(candidate);
            boolean matchesTask = validStandingSpot && matchesTask(ComfortTask.SPACE, candidate);

            if (validStandingSpot) {
                validStandingSpots++;
            }

            debugCandidate("random", ComfortTask.SPACE, candidate, checked, validStandingSpot, matchesTask);

            if (matchesTask) {
                SpaceCandidate spaceCandidate = evaluateSpaceCandidate(origin, candidate, crowdCenter, "random");
                best = chooseBetterSpaceCandidate(best, spaceCandidate);
            }
        }

        if (best != null) {
            SpaceCandidate loggedBest = best;
            int loggedChecked = checked;
            int loggedValidStandingSpots = validStandingSpots;
            debug(() -> "findSpaceTarget success target=" + posString(loggedBest.pos())
                    + " score=" + String.format(java.util.Locale.ROOT, "%.3f", loggedBest.score())
                    + " phase=" + loggedBest.phase()
                    + " checked=" + loggedChecked
                    + " validStandingSpots=" + loggedValidStandingSpots
                    + " details={" + taskCheckDetails(ComfortTask.SPACE, loggedBest.pos()) + "}");
            return loggedBest.pos();
        }

        debug("findSpaceTarget failed origin=" + posString(origin)
                + " checked=" + checked
                + " validStandingSpots=" + validStandingSpots);
        return null;
    }

    private SpaceCandidate evaluateSpaceCandidate(BlockPos origin, BlockPos candidate, Vec3 crowdCenter, String phase) {
        Path path = createPathTo(candidate);
        if (path == null || !path.canReach()) {
            debug("candidate rejected phase=" + phase
                    + " task=space pos=" + posString(candidate)
                    + " reason=no_path");
            return null;
        }

        return new SpaceCandidate(candidate, spaceScore(origin, candidate, crowdCenter), phase);
    }

    private SpaceCandidate chooseBetterSpaceCandidate(SpaceCandidate current, SpaceCandidate candidate) {
        if (candidate == null) {
            return current;
        }

        if (current == null || candidate.score() > current.score()) {
            return candidate;
        }

        return current;
    }

    private double spaceScore(BlockPos origin, BlockPos candidate, Vec3 crowdCenter) {
        Vec3 originCenter = Vec3.atCenterOf(origin);
        Vec3 candidateCenter = Vec3.atCenterOf(candidate);
        double score = -originCenter.distanceTo(candidateCenter);

        if (crowdCenter != null) {
            Vec3 awayDirection = originCenter.subtract(crowdCenter);
            if (awayDirection.horizontalDistanceSqr() > 0.0001D) {
                awayDirection = new Vec3(awayDirection.x, 0.0D, awayDirection.z).normalize();
                Vec3 moveDirection = candidateCenter.subtract(originCenter);
                score += new Vec3(moveDirection.x, 0.0D, moveDirection.z).dot(awayDirection) * 12.0D;
            }

            score += candidateCenter.distanceTo(crowdCenter) * 4.0D;
        }

        return score;
    }

    private int coverScore(BlockPos origin, BlockPos pos) {
        int score = 0;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos oneStep = pos.relative(direction);
            BlockPos twoSteps = oneStep.relative(direction);

            if (isRainCoverSpotForAnimalAt(oneStep)) {
                score += 100;
            }
            if (isRainCoverSpotForAnimalAt(twoSteps)) {
                score += 35;
            }
            if (AnimalWeatherComfort.isRainFallingAt(animal.level(), oneStep)) {
                score -= 60;
            }
        }

        score += Math.min(40, (int) Math.round(Math.sqrt(pos.distSqr(origin))));
        return score;
    }

    private BlockPos findFriendTarget() {
        int radius = Salts_animal_farm.CONFIG.comfortSearchRadius();
        AABB area = animal.getBoundingBox().inflate(radius);
        List<Animal> friends = animal.level().getEntities(
                net.minecraft.world.level.entity.EntityTypeTest.forClass(Animal.class),
                area,
                friend -> friend != animal && friend.isAlive() && friend.getType() == animal.getType()
        );

        if (friends.isEmpty()) {
            debug("findFriendTarget failed no same-type friends radius=" + radius);
            return null;
        }

        Animal friend = friends.get(animal.getRandom().nextInt(friends.size()));
        targetFriend = friend;
        BlockPos friendPos = friend.blockPosition();
        debug(() -> "findFriendTarget found friends=" + friends.size()
                + " chosen=" + friend.getType().toShortString() + "#" + friend.getId()
                + " friendPos=" + posString(friendPos)
                + " distanceSqr=" + String.format(java.util.Locale.ROOT, "%.3f", friend.distanceToSqr(animal)));

        BlockPos target = findStandingSpotNearFriend(friend);
        if (target != null) {
            return target;
        }

        debug(() -> "findFriendTarget failed friend block is not standing spot friendPos=" + posString(friendPos) + " standing={" + standingSpotDetails(friendPos) + "}");
        return null;
    }

    private BlockPos findStandingSpotNearFriend(Animal friend) {
        BlockPos friendPos = friend.blockPosition();

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = friendPos.relative(direction);

            if (isValidStandingSpot(candidate)) {
                debug(() -> "findFriendTarget success adjacent direction=" + direction + " target=" + posString(candidate) + " friendPos=" + posString(friendPos) + " standing={" + standingSpotDetails(candidate) + "}");
                return candidate;
            } else {
                debug(() -> "findFriendTarget rejected adjacent direction=" + direction + " target=" + posString(candidate) + " friendPos=" + posString(friendPos) + " standing={" + standingSpotDetails(candidate) + "}");
            }
        }

        if (isValidStandingSpot(friendPos)) {
            debug(() -> "findFriendTarget using friend's current block target=" + posString(friendPos) + " standing={" + standingSpotDetails(friendPos) + "}");
            return friendPos;
        }

        return null;
    }

    private void updateFriendTarget() {
        if (activeTask != ComfortTask.FRIEND || targetFriend == null || !targetFriend.isAlive()) {
            return;
        }

        BlockPos updatedTarget = findStandingSpotNearFriend(targetFriend);
        if (updatedTarget == null || updatedTarget.equals(targetPos)) {
            return;
        }

        double oldTargetDistanceSqr = blockDistanceSqr(targetPos, updatedTarget);
        if (oldTargetDistanceSqr < FRIEND_RETARGET_DISTANCE_SQR && !animal.getNavigation().isDone()) {
            return;
        }

        debug(() -> "friend target moved, retargeting oldTarget=" + posString(targetPos)
                + " newTarget=" + posString(updatedTarget)
                + " friend=" + targetFriend.getType().toShortString() + "#" + targetFriend.getId()
                + " friendPos=" + posString(targetFriend.blockPosition())
                + " oldTargetDistanceSqr=" + String.format(java.util.Locale.ROOT, "%.3f", oldTargetDistanceSqr));
        targetPos = updatedTarget;
        animal.getNavigation().moveTo(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D, Salts_animal_farm.CONFIG.comfortMoveSpeed());
    }

    private void updateSpaceTarget() {
        if (activeTask != ComfortTask.SPACE || targetPos == null) {
            return;
        }

        boolean targetStillOpen = isValidStandingSpot(targetPos) && matchesTask(ComfortTask.SPACE, targetPos);
        boolean shouldRefresh = !targetStillOpen || activeTicks % SPACE_RETARGET_INTERVAL_TICKS == 0 || animal.getNavigation().isDone();

        if (!shouldRefresh) {
            return;
        }

        BlockPos updatedTarget = findSpaceTarget();
        if (updatedTarget == null) {
            debug(() -> "space target refresh found no open target currentTarget=" + posString(targetPos)
                    + " targetStillOpen=" + targetStillOpen
                    + " currentCheck={" + taskCheckDetails(ComfortTask.SPACE, animal.blockPosition()) + "}"
                    + " targetCheck={" + taskCheckDetails(ComfortTask.SPACE, targetPos) + "}");
            return;
        }

        if (updatedTarget.equals(targetPos) && !animal.getNavigation().isDone()) {
            return;
        }

        debug(() -> "space target refresh oldTarget=" + posString(targetPos)
                + " newTarget=" + posString(updatedTarget)
                + " targetStillOpen=" + targetStillOpen
                + " currentPos=" + posString(animal.blockPosition())
                + " newTargetCheck={" + taskCheckDetails(ComfortTask.SPACE, updatedTarget) + "}");
        targetPos = updatedTarget;
        animal.getNavigation().moveTo(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D, Salts_animal_farm.CONFIG.comfortMoveSpeed());
    }

    private boolean matchesTask(ComfortTask task, BlockPos pos) {
        return switch (task) {
            case SHADE -> !canSeeSkyAt(pos);
            case SUNLIGHT, STARS -> canSeeSkyAt(pos);
            case WATER -> hasAdjacentWater(pos);
            case SPACE -> hasOpenSpace(pos);
            case COVER -> isRainCoverSpotForAnimalAt(pos);
            case LIGHT -> animal.level().getBrightness(LightLayer.BLOCK, pos.above()) >= 10;
            case NAP -> isNapBlock(pos.below());
            case FRIEND -> false;
        };
    }

    private boolean isValidStandingSpot(BlockPos pos) {
        return animal.level().isInWorldBounds(pos)
                && animal.level().isLoaded(pos)
                && isDryStandingSpot(pos)
                && isPassableForAnimal(pos)
                && isPassableForAnimal(pos.above())
                && animal.getNavigation().isStableDestination(pos);
    }

    private boolean canNavigateTo(BlockPos pos) {
        Path path = createPathTo(pos);
        return path != null && path.canReach();
    }

    private Path createPathTo(BlockPos pos) {
        return animal.getNavigation().createPath(pos, 0);
    }

    private boolean isPathExact(Path path, BlockPos pos) {
        return path != null && path.canReach() && path.getEndNode() != null && path.getEndNode().asBlockPos().equals(pos);
    }

    private boolean isCompletionSpot(ComfortTask task, BlockPos pos) {
        if (task == ComfortTask.FRIEND) {
            return hasNearbyFriend();
        }

        if (task == ComfortTask.COVER) {
            return AnimalWeatherComfort.isFullyCovered(animal);
        }

        return matchesTask(task, pos);
    }

    private boolean canCompleteFromTarget(ComfortTask task) {
        return task != ComfortTask.COVER && task != ComfortTask.SPACE && task != ComfortTask.FRIEND;
    }

    private boolean hasNearbyFriend() {
        if (targetFriend != null && targetFriend.isAlive() && targetFriend.getType() == animal.getType()) {
            return animal.distanceToSqr(targetFriend) <= FRIEND_COMPLETION_DISTANCE_SQR;
        }

        AABB area = animal.getBoundingBox().inflate(2.25D, 1.0D, 2.25D);
        return animal.level().hasEntities(
                net.minecraft.world.level.entity.EntityTypeTest.forClass(Animal.class),
                area,
                friend -> friend != animal && friend.isAlive() && friend.getType() == animal.getType()
        );
    }

    private boolean hasDangerInterrupt() {
        return weightedAnimal.salts_animal_farm$isFrantic()
                || animal.hurtTime > 0
                || hasVisibleScaryMob();
    }

    private boolean hasVisibleScaryMob() {
        int radius = Salts_animal_farm.CONFIG.hostileScareRadius();
        AABB area = animal.getBoundingBox().inflate(radius);
        return animal.level().hasEntities(
                EntityTypeTest.forClass(LivingEntity.class),
                area,
                entity -> entity != animal
                        && entity.isAlive()
                        && SaltsAnimalFarmConfigLists.isScaryMob(entity)
                        && animal.getSensing().hasLineOfSight(entity)
        );
    }

    private boolean canSeeSkyAt(BlockPos pos) {
        return animal.level().canSeeSky(pos.above()) || animal.level().canSeeSky(pos.above(2));
    }

    private boolean isRainCoverSpot(BlockPos pos) {
        return isDryStandingSpot(pos)
                && !canSeeSkyAt(pos)
                && AnimalWeatherComfort.isCoveredAt(animal.level(), pos);
    }

    private boolean isRainCoverSpotForAnimalAt(BlockPos pos) {
        AABB box = animal.getBoundingBox();
        double halfX = Math.max((box.maxX - box.minX) * 0.5D - 0.05D, 0.05D);
        double halfZ = Math.max((box.maxZ - box.minZ) * 0.5D - 0.05D, 0.05D);
        double centerX = pos.getX() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;
        double y = pos.getY();

        return isRainCoverSpot(BlockPos.containing(centerX - halfX, y, centerZ - halfZ))
                && isRainCoverSpot(BlockPos.containing(centerX - halfX, y, centerZ + halfZ))
                && isRainCoverSpot(BlockPos.containing(centerX + halfX, y, centerZ - halfZ))
                && isRainCoverSpot(BlockPos.containing(centerX + halfX, y, centerZ + halfZ))
                && isRainCoverSpot(pos);
    }

    private boolean isDryStandingSpot(BlockPos pos) {
        return !isWater(pos) && !isWater(pos.above()) && !isWater(pos.below());
    }

    private boolean isPassableForAnimal(BlockPos pos) {
        BlockState state = animal.level().getBlockState(pos);
        return state.isAir()
                || state.canBeReplaced()
                || state.isPathfindable(PathComputationType.LAND)
                || state.getCollisionShape(animal.level(), pos).isEmpty();
    }

    private boolean hasAdjacentWater(BlockPos pos) {
        if (isWater(pos) || isWater(pos.below())) {
            return true;
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos adjacent = pos.relative(direction);
            if (isWater(adjacent) || isWater(adjacent.below())) {
                return true;
            }
        }

        return false;
    }

    private boolean isWater(BlockPos pos) {
        return animal.level().getFluidState(pos).typeHolder().is(FluidTags.WATER);
    }

    private boolean hasOpenSpace(BlockPos pos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos floor = pos.offset(x, 0, z);

                if (!isPassableForAnimal(floor) || !isPassableForAnimal(floor.above())) {
                    return false;
                }
            }
        }

        AABB area = centeredAabb(pos).inflate(SPACE_MOB_CLEAR_RADIUS, 1.0D, SPACE_MOB_CLEAR_RADIUS);
        if (animal.level().hasEntities(
                net.minecraft.world.level.entity.EntityTypeTest.forClass(Animal.class),
                centeredAabb(pos).inflate(SPACE_CLEAR_RADIUS, 1.5D, SPACE_CLEAR_RADIUS),
                nearbyAnimal -> nearbyAnimal != animal && nearbyAnimal.isAlive()
        )) {
            return false;
        }

        return !animal.level().hasEntities(
                net.minecraft.world.level.entity.EntityTypeTest.forClass(Mob.class),
                area,
                mob -> mob != animal && mob.isAlive()
        );
    }

    private boolean isNapBlock(BlockPos pos) {
        BlockState state = animal.level().getBlockState(pos);
        return SaltsAnimalFarmConfigLists.isSoftBlock(state);
    }

    private void completeTask(String reason) {
        if (activeTask == ComfortTask.COVER) {
            recordNeutralTaskCompletion(reason);
        } else {
            recordTaskSuccess(reason);
        }

        activeTask = null;
        targetPos = null;
    }

    private void recordNeutralTaskCompletion(String reason) {
        debug("completeTask neutral reason=" + reason
                + " task=" + taskNameOrNull()
                + " target=" + posString(targetPos)
                + " activeTicks=" + activeTicks
                + " lingerTicks=" + lingerTicks
                + " weight=" + weightedAnimal.salts_animal_farm$getWeight()
                + " streak=" + weightedAnimal.salts_animal_farm$getSuccessfulTaskStreak()
                + " rainExposureTicks=" + weightedAnimal.salts_animal_farm$getRainExposureTicks());
        playTaskSuccessFeedback();
        weightedAnimal.salts_animal_farm$setLastComfortTask(taskNameOrNull());
        weightedAnimal.salts_animal_farm$setLastComfortTaskResult("Covered");
        weightedAnimal.salts_animal_farm$setCurrentComfortTask("");
    }

    private void recordTaskSuccess(String reason) {
        debug("completeTask success reason=" + reason
                + " task=" + taskNameOrNull()
                + " target=" + posString(targetPos)
                + " activeTicks=" + activeTicks
                + " lingerTicks=" + lingerTicks
                + " weightBefore=" + weightedAnimal.salts_animal_farm$getWeight()
                + " streakBefore=" + weightedAnimal.salts_animal_farm$getSuccessfulTaskStreak()
                + " totalSuccessBefore=" + weightedAnimal.salts_animal_farm$getTotalSuccessfulTasks());
        weightedAnimal.salts_animal_farm$recordComfortSuccess();
        playTaskSuccessFeedback();
        debug("completeTask recorded success weightAfter=" + weightedAnimal.salts_animal_farm$getWeight()
                + " streakAfter=" + weightedAnimal.salts_animal_farm$getSuccessfulTaskStreak()
                + " totalSuccessAfter=" + weightedAnimal.salts_animal_farm$getTotalSuccessfulTasks());
    }

    private void recordTaskFailure() {
        weightedAnimal.salts_animal_farm$recordComfortFailure();
        playTaskFailureFeedback();
    }

    private void playTaskSuccessFeedback() {
        if (animal.level() instanceof ServerLevel level) {
            level.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    animal.getX(),
                    animal.getY() + animal.getBbHeight() + 0.25D,
                    animal.getZ(),
                    8,
                    0.35D,
                    0.25D,
                    0.35D,
                    0.02D
            );
        }

        animal.playAmbientSound();
    }

    private void playTaskFailureFeedback() {
        if (animal.level() instanceof ServerLevel level) {
            level.sendParticles(
                    ParticleTypes.ANGRY_VILLAGER,
                    animal.getX(),
                    animal.getY() + animal.getBbHeight() + 0.25D,
                    animal.getZ(),
                    6,
                    0.25D,
                    0.2D,
                    0.25D,
                    0.01D
            );
        }

        animal.makeSound(((LivingEntitySoundAccessor) animal).salts_animal_farm$getHurtSound(animal.damageSources().generic()));
    }

    private void scheduleNextAttempt() {
        SaltsAnimalFarmConfig config = Salts_animal_farm.CONFIG;
        int jitter = config.comfortTaskDelayJitterTicks();
        int minimumDelay = Math.max(20, config.comfortTaskAverageDelayTicks() - jitter);
        int randomExtra = jitter <= 0 ? 0 : animal.getRandom().nextInt(jitter * 2 + 1);
        nextAttemptTick = animal.tickCount + minimumDelay + randomExtra;
        debug("scheduleNextAttempt nextAttemptTick=" + nextAttemptTick
                + " currentTick=" + animal.tickCount
                + " minimumDelay=" + minimumDelay
                + " randomExtra=" + randomExtra
                + " averageDelay=" + config.comfortTaskAverageDelayTicks()
                + " jitter=" + jitter);
    }

    private void scheduleNextRainCoverAttempt() {
        nextRainCoverAttemptTick = animal.tickCount + 100 + animal.getRandom().nextInt(41);
        debug("scheduleNextRainCoverAttempt nextRainCoverAttemptTick=" + nextRainCoverAttemptTick
                + " currentTick=" + animal.tickCount);
    }

    private void debugCandidate(String phase, ComfortTask task, BlockPos candidate, int checked, boolean validStandingSpot, boolean matchesTask) {
        if (!debugEnabled()) {
            return;
        }

        debug("candidate phase=" + phase
                + " checked=" + checked
                + " task=" + task.taskName()
                + " pos=" + posString(candidate)
                + " validStandingSpot=" + validStandingSpot
                + " matchesTask=" + matchesTask
                + " standing={" + standingSpotDetails(candidate) + "}"
                + " taskCheck={" + taskCheckDetails(task, candidate) + "}");
    }

    private String taskCheckDetails(ComfortTask task, BlockPos pos) {
        if (pos == null) {
            return "pos=null";
        }

        return switch (task) {
            case SHADE -> {
                boolean sky = canSeeSkyAt(pos);
                yield "shade skyAtPos=" + sky + " result=" + !sky + " aboveState=" + blockName(pos.above()) + " above2State=" + blockName(pos.above(2));
            }
            case SUNLIGHT, STARS -> {
                boolean sky = canSeeSkyAt(pos);
                yield task.taskName() + " skyAtPos=" + sky + " result=" + sky + " aboveState=" + blockName(pos.above()) + " above2State=" + blockName(pos.above(2));
            }
            case WATER -> "water adjacentWater=" + hasAdjacentWater(pos)
                    + " selfFluid=" + fluidDebug(pos)
                    + " belowFluid=" + fluidDebug(pos.below())
                    + " northFluid=" + fluidDebug(pos.north())
                    + " northBelowFluid=" + fluidDebug(pos.north().below())
                    + " southFluid=" + fluidDebug(pos.south())
                    + " southBelowFluid=" + fluidDebug(pos.south().below())
                    + " eastFluid=" + fluidDebug(pos.east())
                    + " eastBelowFluid=" + fluidDebug(pos.east().below())
                    + " westFluid=" + fluidDebug(pos.west())
                    + " westBelowFluid=" + fluidDebug(pos.west().below());
            case SPACE -> "space openSpace=" + hasOpenSpace(pos) + " detail=" + openSpaceDetails(pos);
            case COVER -> "cover coveredAtPos=" + AnimalWeatherComfort.isCoveredAt(animal.level(), pos)
                    + " rainingInRainBiome=" + AnimalWeatherComfort.isRainingInRainBiome(animal)
                    + " rainFallingAtPos=" + AnimalWeatherComfort.isRainFallingAt(animal.level(), pos)
                    + " dryStanding=" + isDryStandingSpot(pos)
                    + " underBlock=" + !canSeeSkyAt(pos)
                    + " fullyUnderCover=" + AnimalWeatherComfort.isFullyCovered(animal)
                    + " aboveState=" + blockName(pos.above())
                    + " above2State=" + blockName(pos.above(2));
            case LIGHT -> {
                int blockLight = animal.level().getBrightness(LightLayer.BLOCK, pos.above());
                yield "light blockLightAbove=" + blockLight + " result=" + (blockLight >= 10) + " aboveState=" + blockName(pos.above());
            }
            case NAP -> "nap below=" + blockName(pos.below()) + " softBlock=" + isNapBlock(pos.below());
            case FRIEND -> "friend nearbyFriendCount=" + nearbyFriendCount() + " result=" + hasNearbyFriend();
        };
    }

    private String standingSpotDetails(BlockPos pos) {
        if (pos == null) {
            return "pos=null";
        }

        boolean inWorld = animal.level().isInWorldBounds(pos);
        boolean loaded = inWorld && animal.level().isLoaded(pos);
        boolean dry = loaded && isDryStandingSpot(pos);
        boolean passable = loaded && isPassableForAnimal(pos);
        boolean abovePassable = loaded && isPassableForAnimal(pos.above());
        boolean stable = loaded && animal.getNavigation().isStableDestination(pos);
        return "inWorld=" + inWorld
                + " loaded=" + loaded
                + " block=" + blockName(pos)
                + " above=" + blockName(pos.above())
                + " dryStanding=" + dry
                + " passable=" + passable
                + " abovePassable=" + abovePassable
                + " stableDestination=" + stable;
    }

    private String openSpaceDetails(BlockPos pos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos floor = pos.offset(x, 0, z);

                if (!isPassableForAnimal(floor)) {
                    return "blockedFloor pos=" + posString(floor) + " block=" + blockName(floor);
                }

                if (!isPassableForAnimal(floor.above())) {
                    return "blockedAbove pos=" + posString(floor.above()) + " block=" + blockName(floor.above());
                }
            }
        }

        int mobCount = nearbySpaceMobCount(pos);
        int animalCount = nearbySpaceAnimalCount(pos);
        if (animalCount > 0) {
            return "nearbyAnimalCount=" + animalCount + " clearRadius=" + SPACE_CLEAR_RADIUS;
        }

        return mobCount == 0 ? "clear" : "nearbyMobCount=" + mobCount;
    }

    private int nearbySpaceAnimalCount(BlockPos pos) {
        AABB area = centeredAabb(pos).inflate(SPACE_CLEAR_RADIUS, 1.5D, SPACE_CLEAR_RADIUS);
        return animal.level().getEntities(
                net.minecraft.world.level.entity.EntityTypeTest.forClass(Animal.class),
                area,
                nearbyAnimal -> nearbyAnimal != animal && nearbyAnimal.isAlive()
        ).size();
    }

    private int nearbySpaceMobCount(BlockPos pos) {
        AABB area = centeredAabb(pos).inflate(SPACE_MOB_CLEAR_RADIUS, 1.0D, SPACE_MOB_CLEAR_RADIUS);
        return animal.level().getEntities(
                net.minecraft.world.level.entity.EntityTypeTest.forClass(Mob.class),
                area,
                mob -> mob != animal && mob.isAlive()
        ).size();
    }

    private Vec3 nearbyAnimalCenter(double radius) {
        AABB area = animal.getBoundingBox().inflate(radius);
        List<Animal> nearbyAnimals = animal.level().getEntities(
                net.minecraft.world.level.entity.EntityTypeTest.forClass(Animal.class),
                area,
                nearbyAnimal -> nearbyAnimal != animal && nearbyAnimal.isAlive()
        );

        if (nearbyAnimals.isEmpty()) {
            return null;
        }

        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;

        for (Animal nearbyAnimal : nearbyAnimals) {
            x += nearbyAnimal.getX();
            y += nearbyAnimal.getY();
            z += nearbyAnimal.getZ();
        }

        return new Vec3(x / nearbyAnimals.size(), y / nearbyAnimals.size(), z / nearbyAnimals.size());
    }

    private int nearbyFriendCount() {
        AABB area = animal.getBoundingBox().inflate(2.25D, 1.0D, 2.25D);
        return animal.level().getEntities(
                net.minecraft.world.level.entity.EntityTypeTest.forClass(Animal.class),
                area,
                friend -> friend != animal && friend.isAlive() && friend.getType() == animal.getType()
        ).size();
    }

    private String fluidDebug(BlockPos pos) {
        return animal.level().getFluidState(pos).typeHolder().is(FluidTags.WATER) ? "water" : "not_water";
    }

    private String blockName(BlockPos pos) {
        if (!animal.level().isInWorldBounds(pos) || !animal.level().isLoaded(pos)) {
            return "unloaded_or_out_of_world";
        }

        BlockState state = animal.level().getBlockState(pos);
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()) + "[air=" + state.isAir()
                + ",replaceable=" + state.canBeReplaced()
                + ",pathfindable=" + state.isPathfindable(PathComputationType.LAND)
                + ",emptyCollision=" + state.getCollisionShape(animal.level(), pos).isEmpty()
                + "]";
    }

    private String taskNameOrNull() {
        return activeTask == null ? "Null" : activeTask.taskName();
    }

    private boolean debugEnabled() {
        return Salts_animal_farm.CONFIG.enableDetailedDebugInformation();
    }

    private void debug(String message) {
        if (!debugEnabled()) {
            return;
        }

        Salts_animal_farm.LOGGER.info("[AnimalComfort] {} {}", animalDebugName(), message);
    }

    private void debug(Supplier<String> messageSupplier) {
        if (!debugEnabled()) {
            return;
        }

        Salts_animal_farm.LOGGER.info("[AnimalComfort] {} {}", animalDebugName(), messageSupplier.get());
    }

    private String animalDebugName() {
        return animal.getType().toShortString()
                + "#" + animal.getId()
                + " uuid=" + animal.getUUID()
                + " tick=" + animal.tickCount
                + " pos=" + posString(animal.blockPosition())
                + " weight=" + weightedAnimal.salts_animal_farm$getWeight()
                + " streak=" + weightedAnimal.salts_animal_farm$getSuccessfulTaskStreak()
                + " success=" + weightedAnimal.salts_animal_farm$getTotalSuccessfulTasks()
                + " fail=" + weightedAnimal.salts_animal_farm$getTotalFailedTasks();
    }

    private static String posString(BlockPos pos) {
        return pos == null ? "null" : "(" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + ")";
    }

    private static String vecString(Vec3 vec) {
        return vec == null
                ? "null"
                : "(" + String.format(java.util.Locale.ROOT, "%.2f", vec.x)
                + "," + String.format(java.util.Locale.ROOT, "%.2f", vec.y)
                + "," + String.format(java.util.Locale.ROOT, "%.2f", vec.z) + ")";
    }

    private double distanceToCenterSqr(BlockPos pos) {
        double dx = animal.getX() - (pos.getX() + 0.5D);
        double dy = animal.getY() - (pos.getY() + 0.5D);
        double dz = animal.getZ() - (pos.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }

    private static double blockDistanceSqr(BlockPos first, BlockPos second) {
        double dx = first.getX() - second.getX();
        double dy = first.getY() - second.getY();
        double dz = first.getZ() - second.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private static AABB centeredAabb(BlockPos pos) {
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;
        return new AABB(centerX, centerY, centerZ, centerX, centerY, centerZ);
    }

    public enum ComfortTask {
        SHADE("shade"),
        SUNLIGHT("sunlight"),
        WATER("water"),
        SPACE("space"),
        FRIEND("friend"),
        COVER("cover"),
        LIGHT("light"),
        NAP("nap"),
        STARS("stars");

        private static final String[] NAMES = java.util.Arrays.stream(values()).map(ComfortTask::taskName).toArray(String[]::new);
        private final String taskName;

        ComfortTask(String taskName) {
            this.taskName = taskName;
        }

        public String taskName() {
            return taskName;
        }

        public static String[] taskNames() {
            return NAMES;
        }

        public static ComfortTask byName(String name) {
            for (ComfortTask task : values()) {
                if (task.taskName.equalsIgnoreCase(name)) {
                    return task;
                }
            }

            return null;
        }
    }

    private record CoverCandidate(BlockPos pos, int score) {
    }

    private record SpaceCandidate(BlockPos pos, double score, String phase) {
    }
}
