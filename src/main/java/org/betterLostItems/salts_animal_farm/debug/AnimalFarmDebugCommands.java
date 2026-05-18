package org.betterLostItems.salts_animal_farm.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.betterLostItems.salts_animal_farm.api.WeightedFarmAnimal;
import org.betterLostItems.salts_animal_farm.entity.SaltsAnimalFarmConfigLists;
import org.betterLostItems.salts_animal_farm.entity.ai.AnimalComfortGoal;
import org.betterLostItems.salts_animal_farm.network.RenderDebugFarmDataPayload;

import java.util.Comparator;
import java.util.List;

public final class AnimalFarmDebugCommands {
    private static final double NEARBY_RADIUS = 32.0D;
    private static final double LOOK_DISTANCE = 8.0D;

    private AnimalFarmDebugCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("animalfarm")
                .then(Commands.literal("render_debug_farm_data")
                        .then(Commands.argument("visible", BoolArgumentType.bool())
                                .executes(context -> setRenderDebugFarmData(context, BoolArgumentType.getBool(context, "visible")))))
                .then(Commands.literal("task")
                        .executes(context -> forceTask(context, "", -1))
                        .then(Commands.argument("task_name", StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(AnimalComfortGoal.ComfortTask.taskNames(), builder))
                                .executes(context -> forceTask(context, StringArgumentType.getString(context, "task_name"), -1))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(context -> forceTask(context, StringArgumentType.getString(context, "task_name"), IntegerArgumentType.getInteger(context, "amount"))))))
                        .then(Commands.literal("weight")
                                .then(Commands.literal("add")
                                        .executes(context -> changeLookedAtWeight(context, WeightMode.ADD, 1))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(context -> changeLookedAtWeight(context, WeightMode.ADD, IntegerArgumentType.getInteger(context, "amount")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("value", IntegerArgumentType.integer(WeightedFarmAnimal.MIN_WEIGHT, WeightedFarmAnimal.MAX_WEIGHT))
                                        .executes(context -> changeLookedAtWeight(context, WeightMode.SET, IntegerArgumentType.getInteger(context, "value")))))
                        .then(Commands.literal("subtract")
                                .executes(context -> changeLookedAtWeight(context, WeightMode.SUBTRACT, 1))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(context -> changeLookedAtWeight(context, WeightMode.SUBTRACT, IntegerArgumentType.getInteger(context, "amount")))))));
    }

    private static int setRenderDebugFarmData(CommandContext<CommandSourceStack> context, boolean visible) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        if (ServerPlayNetworking.canSend(player, RenderDebugFarmDataPayload.TYPE)) {
            ServerPlayNetworking.send(player, new RenderDebugFarmDataPayload(visible));
        }

        AnimalFarmDebugDataSender.setEnabled(player, visible);
        context.getSource().sendSuccess(() -> Component.literal("Animal Farm debug data rendering: " + visible), false);
        return visible ? 1 : 0;
    }

    private static int forceTask(CommandContext<CommandSourceStack> context, String taskName, int limit) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        if (!taskName.isBlank() && AnimalComfortGoal.ComfortTask.byName(taskName) == null) {
            context.getSource().sendFailure(Component.literal("Unknown animal task '" + taskName + "'."));
            return 0;
        }

        List<Animal> animals = nearbyFarmAnimals(player);
        int changed = 0;

        for (Animal animal : animals) {
            if (limit > -1 && changed >= limit) {
                break;
            }

            if (animal instanceof WeightedFarmAnimal weightedAnimal) {
                weightedAnimal.salts_animal_farm$forceComfortTask(taskName);
                changed++;
            }
        }

        String label = taskName.isBlank() ? "random" : taskName;
        int finalChanged = changed;
        context.getSource().sendSuccess(() -> Component.literal("Forced " + finalChanged + " nearby farm animals to start task: " + label), false);
        return changed;
    }

    private static int changeLookedAtWeight(CommandContext<CommandSourceStack> context, WeightMode mode, int amount) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Animal animal = lookedAtFarmAnimal(player);

        if (animal == null || !(animal instanceof WeightedFarmAnimal weightedAnimal)) {
            context.getSource().sendFailure(Component.literal("Look at a configured farm animal within " + (int) LOOK_DISTANCE + " blocks."));
            return 0;
        }

        int oldWeight = weightedAnimal.salts_animal_farm$getWeight();

        switch (mode) {
            case ADD -> weightedAnimal.salts_animal_farm$addWeight(amount);
            case SET -> weightedAnimal.salts_animal_farm$setWeight(amount);
            case SUBTRACT -> weightedAnimal.salts_animal_farm$addWeight(-amount);
        }

        int newWeight = weightedAnimal.salts_animal_farm$getWeight();
        context.getSource().sendSuccess(() -> Component.literal("Changed " + animal.getType().toShortString() + " weight from " + oldWeight + " to " + newWeight + "."), false);
        return newWeight;
    }

    private static List<Animal> nearbyFarmAnimals(ServerPlayer player) {
        ServerLevel level = player.level();
        AABB area = player.getBoundingBox().inflate(NEARBY_RADIUS);
        List<Animal> animals = level.getEntities(
                EntityTypeTest.forClass(Animal.class),
                area,
                animal -> animal.isAlive() && SaltsAnimalFarmConfigLists.isFarmAnimal(animal)
        );

        animals.sort(Comparator.comparingDouble(animal -> animal.distanceToSqr(player)));
        return animals;
    }

    private static Animal lookedAtFarmAnimal(ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(LOOK_DISTANCE));
        AABB area = player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
                player,
                start,
                end,
                area,
                entity -> entity instanceof Animal animal && animal.isAlive() && SaltsAnimalFarmConfigLists.isFarmAnimal(animal),
                LOOK_DISTANCE * LOOK_DISTANCE
        );

        if (hitResult == null) {
            return null;
        }

        Entity entity = hitResult.getEntity();
        return entity instanceof Animal animal ? animal : null;
    }

    private enum WeightMode {
        ADD,
        SET,
        SUBTRACT
    }
}
