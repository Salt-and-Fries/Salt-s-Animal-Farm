package org.betterLostItems.salts_animal_farm.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public record SaltsAnimalFarmConfig(
        Boolean enableMod,
        @SerializedName("Farm Animals")
        List<String> farmAnimals,
        @SerializedName("Scary Mobs")
        List<String> scaryMobs,
        @SerializedName("Soft Blocks")
        List<String> softBlocks,
        int minimumWeight,
        int maximumWeight,
        Boolean enableRainBehavior,
        int comfortTaskAverageDelayTicks,
        int comfortTaskDelayJitterTicks,
        int comfortSearchRadius,
        int comfortVerticalSearch,
        int comfortSearchSamples,
        int comfortLingerTicks,
        int comfortTaskReachTimeoutTicks,
        int comfortMaxTaskTicks,
        double comfortMoveSpeed,
        int hostileScareRadius,
        int hostileScanIntervalTicks,
        int hostileScanRandomOffsetTicks,
        int hostileScareCooldownTicks,
        double hostileFleeSpeed,
        int killWitnessRadius,
        int maxKillWitnesses,
        int franticDurationTicks,
        int franticRepathTicks,
        double franticMoveSpeed,
        @SerializedName("enable_detailed_debug_information")
        boolean enableDetailedDebugInformation
) {
    public static final SaltsAnimalFarmConfig DEFAULT = new SaltsAnimalFarmConfig(
            true,
            List.of(
                    "minecraft:cow",
                    "minecraft:mooshroom",
                    "minecraft:pig",
                    "minecraft:sheep",
                    "minecraft:chicken",
                    "minecraft:rabbit"
            ),
            List.of(
                    "#minecraft:skeletons",
                    "#minecraft:arthropod",
                    "#minecraft:illager",
                    "minecraft:zombie",
                    "minecraft:zombie_villager",
                    "minecraft:husk",
                    "minecraft:drowned",
                    "minecraft:creeper",
                    "minecraft:enderman",
                    "minecraft:witch",
                    "minecraft:ravager",
                    "minecraft:warden"
            ),
            List.of(
                    "#minecraft:wool",
                    "#minecraft:wool_carpets",
                    "minecraft:hay_block",
                    "minecraft:clay",
                    "minecraft:moss_block",
                    "minecraft:moss_carpet",
                    "minecraft:pale_moss_block",
                    "minecraft:pale_moss_carpet"
            ),
            1,
            8,
            true,
            4000,
            2000,
            12,
            4,
            28,
            20,
            200,
            600,
            1.0,
            16,
            40,
            20,
            200,
            1.3,
            32,
            64,
            200,
            20,
            1.35,
            false
    );

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static SaltsAnimalFarmConfig load() {
        Path path = configPath();

        if (Files.notExists(path)) {
            writeDefault(path);
            return DEFAULT;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            SaltsAnimalFarmConfig config = GSON.fromJson(reader, SaltsAnimalFarmConfig.class);
            SaltsAnimalFarmConfig sanitized = config == null ? DEFAULT : config.sanitized();
            writeConfig(path, sanitized);
            return sanitized;
        } catch (IOException | RuntimeException exception) {
            Salts_animal_farm.LOGGER.warn("Failed to load Salt's Animal Farm config, using defaults", exception);
            return DEFAULT;
        }
    }

    private static void writeDefault(Path path) {
        writeConfig(path, DEFAULT);
    }

    public static void save(SaltsAnimalFarmConfig config) {
        writeConfig(configPath(), config.sanitized());
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("salts_animal_farm.json");
    }

    private static void writeConfig(Path path, SaltsAnimalFarmConfig config) {
        try {
            Files.createDirectories(path.getParent());

            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException exception) {
            Salts_animal_farm.LOGGER.warn("Failed to write default Salt's Animal Farm config", exception);
        }
    }

    public SaltsAnimalFarmConfig sanitized() {
        int sanitizedMinimumWeight = Math.max(minimumWeight, 0);
        int sanitizedMaximumWeight = maximumWeight <= sanitizedMinimumWeight
                ? Math.max(DEFAULT.maximumWeight, sanitizedMinimumWeight)
                : maximumWeight;

        return new SaltsAnimalFarmConfig(
                enableMod == null ? DEFAULT.modEnabled() : enableMod,
                sanitizedList(farmAnimals, DEFAULT.farmAnimals),
                sanitizedList(scaryMobs, DEFAULT.scaryMobs),
                sanitizedList(softBlocks, DEFAULT.softBlocks),
                sanitizedMinimumWeight,
                sanitizedMaximumWeight,
                enableRainBehavior == null ? DEFAULT.rainBehaviorEnabled() : enableRainBehavior,
                atLeast(comfortTaskAverageDelayTicks, 200),
                Math.max(comfortTaskDelayJitterTicks, 0),
                atLeast(comfortSearchRadius, 2),
                atLeast(comfortVerticalSearch, 1),
                atLeast(comfortSearchSamples, 4),
                atLeast(comfortLingerTicks, 1),
                atLeast(comfortTaskReachTimeoutTicks, 20),
                atLeast(comfortMaxTaskTicks, 20),
                atLeast(comfortMoveSpeed, 0.1),
                atLeast(hostileScareRadius, 4),
                atLeast(hostileScanIntervalTicks, 5),
                Math.max(hostileScanRandomOffsetTicks, 0),
                atLeast(hostileScareCooldownTicks, 1),
                atLeast(hostileFleeSpeed, 0.1),
                atLeast(killWitnessRadius, 4),
                atLeast(maxKillWitnesses, 1),
                atLeast(franticDurationTicks, 20),
                atLeast(franticRepathTicks, 5),
                atLeast(franticMoveSpeed, 0.1),
                enableDetailedDebugInformation
        );
    }

    public boolean rainBehaviorEnabled() {
        return Boolean.TRUE.equals(enableRainBehavior);
    }

    public boolean modEnabled() {
        return Boolean.TRUE.equals(enableMod);
    }

    private static int atLeast(int value, int minimum) {
        return Math.max(value, minimum);
    }

    private static double atLeast(double value, double minimum) {
        return Math.max(value, minimum);
    }

    private static List<String> sanitizedList(List<String> values, List<String> fallback) {
        if (values == null) {
            return fallback;
        }

        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
    }
}
