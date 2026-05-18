package org.betterLostItems.salts_animal_farm.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;

public final class AnimalWeatherComfort {
    public static final int RAIN_COVER_WEIGHT_LOSS_TICKS = 2400;

    private AnimalWeatherComfort() {
    }

    public static boolean shouldSeekRainCover(Animal animal) {
        Level level = animal.level();
        return level.isRaining()
                && level.precipitationAt(animal.blockPosition()) == Biome.Precipitation.RAIN
                && !isFullyCovered(animal);
    }

    public static boolean isRainingInRainBiome(Animal animal) {
        return animal.level().isRaining() && animal.level().precipitationAt(animal.blockPosition()) == Biome.Precipitation.RAIN;
    }

    public static boolean isRainFallingAt(Level level, BlockPos pos) {
        return isRainFallingAt(level, pos, level.isRaining());
    }

    private static boolean isRainFallingAt(Level level, BlockPos pos, boolean raining) {
        return raining
                && level.precipitationAt(pos) == Biome.Precipitation.RAIN
                && (level.canSeeSky(pos.above()) || level.canSeeSky(pos.above(2)));
    }

    public static boolean isCoveredAt(Level level, BlockPos pos) {
        return !isRainFallingAt(level, pos);
    }

    private static boolean isCoveredAt(Level level, BlockPos pos, boolean raining) {
        return !isRainFallingAt(level, pos, raining);
    }

    public static boolean isFullyCovered(Animal animal) {
        AABB box = animal.getBoundingBox();
        double minX = box.minX + 0.05D;
        double maxX = box.maxX - 0.05D;
        double minZ = box.minZ + 0.05D;
        double maxZ = box.maxZ - 0.05D;
        double y = box.minY;

        if (minX > maxX) {
            minX = maxX = animal.getX();
        }
        if (minZ > maxZ) {
            minZ = maxZ = animal.getZ();
        }

        Level level = animal.level();
        boolean raining = level.isRaining();
        return isCoveredAt(level, BlockPos.containing(minX, y, minZ), raining)
                && isCoveredAt(level, BlockPos.containing(minX, y, maxZ), raining)
                && isCoveredAt(level, BlockPos.containing(maxX, y, minZ), raining)
                && isCoveredAt(level, BlockPos.containing(maxX, y, maxZ), raining)
                && isCoveredAt(level, animal.blockPosition(), raining);
    }
}
