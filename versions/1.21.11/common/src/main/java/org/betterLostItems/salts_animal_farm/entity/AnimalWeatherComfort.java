package org.betterLostItems.salts_animal_farm.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class AnimalWeatherComfort {
    public static final int RAIN_COVER_WEIGHT_LOSS_TICKS = 2400;
    private static final int MAX_RAIN_FALLING_CACHE_ENTRIES = 2048;
    private static final Map<RainCacheKey, Boolean> RAIN_FALLING_CACHE = new HashMap<>();
    private static final Map<Animal, FullCoverCache> FULL_COVER_CACHE = new WeakHashMap<>();
    private static long rainCacheTick = Long.MIN_VALUE;

    private AnimalWeatherComfort() {
    }

    public static boolean shouldSeekRainCover(Animal animal) {
        if (!Salts_animal_farm.CONFIG.modEnabled() || !Salts_animal_farm.CONFIG.rainBehaviorEnabled()) {
            return false;
        }

        Level level = animal.level();
        return level.isRaining()
                && level.precipitationAt(animal.blockPosition()) == Biome.Precipitation.RAIN
                && !isFullyCovered(animal);
    }

    public static boolean isRainingInRainBiome(Animal animal) {
        return Salts_animal_farm.CONFIG.modEnabled()
                && Salts_animal_farm.CONFIG.rainBehaviorEnabled()
                && animal.level().isRaining()
                && animal.level().precipitationAt(animal.blockPosition()) == Biome.Precipitation.RAIN;
    }

    public static boolean isRainFallingAt(Level level, BlockPos pos) {
        if (!Salts_animal_farm.CONFIG.modEnabled() || !Salts_animal_farm.CONFIG.rainBehaviorEnabled()) {
            return false;
        }

        return isRainFallingAt(level, pos, level.isRaining());
    }

    private static boolean isRainFallingAt(Level level, BlockPos pos, boolean raining) {
        if (!raining) {
            return false;
        }

        long tick = level.getGameTime();
        if (rainCacheTick != tick) {
            RAIN_FALLING_CACHE.clear();
            rainCacheTick = tick;
        }

        RainCacheKey key = new RainCacheKey(level, pos.getX(), pos.getY(), pos.getZ(), true);
        Boolean cached = RAIN_FALLING_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        boolean result = level.precipitationAt(pos) == Biome.Precipitation.RAIN
                && (hasOpenSkyProbe(level, pos.above()) || hasOpenSkyProbe(level, pos.above(2)));
        if (RAIN_FALLING_CACHE.size() < MAX_RAIN_FALLING_CACHE_ENTRIES) {
            RAIN_FALLING_CACHE.put(key, result);
        }
        return result;
    }

    private static boolean hasOpenSkyProbe(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return (state.isAir() || state.canBeReplaced() || state.getCollisionShape(level, pos).isEmpty())
                && level.canSeeSky(pos);
    }

    public static boolean isCoveredAt(Level level, BlockPos pos) {
        return !isRainFallingAt(level, pos);
    }

    private static boolean isCoveredAt(Level level, BlockPos pos, boolean raining) {
        return !isRainFallingAt(level, pos, raining);
    }

    public static boolean isFullyCovered(Animal animal) {
        if (!Salts_animal_farm.CONFIG.modEnabled() || !Salts_animal_farm.CONFIG.rainBehaviorEnabled()) {
            return true;
        }

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
        long tick = level.getGameTime();
        BlockPos animalPos = animal.blockPosition();
        FullCoverCache cached = FULL_COVER_CACHE.get(animal);

        if (cached != null && cached.tick == tick && cached.raining == raining && cached.pos.equals(animalPos)) {
            return cached.result;
        }

        boolean result = isCoveredAt(level, BlockPos.containing(minX, y, minZ), raining)
                && isCoveredAt(level, BlockPos.containing(minX, y, maxZ), raining)
                && isCoveredAt(level, BlockPos.containing(maxX, y, minZ), raining)
                && isCoveredAt(level, BlockPos.containing(maxX, y, maxZ), raining)
                && isCoveredAt(level, animal.blockPosition(), raining);
        FULL_COVER_CACHE.put(animal, new FullCoverCache(tick, animalPos.immutable(), raining, result));
        return result;
    }

    private record RainCacheKey(Level level, int x, int y, int z, boolean raining) {
    }

    private record FullCoverCache(long tick, BlockPos pos, boolean raining, boolean result) {
    }
}
