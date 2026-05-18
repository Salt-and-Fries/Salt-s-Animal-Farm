package org.betterLostItems.salts_animal_farm.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;

public final class AnimalWeatherComfort {
    public static final int RAIN_COVER_WEIGHT_LOSS_TICKS = 2400;

    private AnimalWeatherComfort() {
    }

    public static boolean isCowLike(Animal animal) {
        return animal.getType() == EntityType.COW || animal.getType() == EntityType.MOOSHROOM;
    }

    public static boolean shouldSeekRainCover(Animal animal) {
        return isCowLike(animal) && isRainingInRainBiome(animal) && !isFullyCovered(animal);
    }

    public static boolean isRainingInRainBiome(Animal animal) {
        return animal.level().isRaining() && animal.level().precipitationAt(animal.blockPosition()) == Biome.Precipitation.RAIN;
    }

    public static boolean isRainFallingAt(Level level, BlockPos pos) {
        return level.isRaining()
                && level.precipitationAt(pos) == Biome.Precipitation.RAIN
                && (level.canSeeSky(pos.above()) || level.canSeeSky(pos.above(2)));
    }

    public static boolean isCoveredAt(Level level, BlockPos pos) {
        return !isRainFallingAt(level, pos);
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
        return isCoveredAt(level, BlockPos.containing(minX, y, minZ))
                && isCoveredAt(level, BlockPos.containing(minX, y, maxZ))
                && isCoveredAt(level, BlockPos.containing(maxX, y, minZ))
                && isCoveredAt(level, BlockPos.containing(maxX, y, maxZ))
                && isCoveredAt(level, animal.blockPosition());
    }
}
