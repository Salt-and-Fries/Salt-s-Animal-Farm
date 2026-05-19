package org.betterLostItems.salts_animal_farm.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;
import org.betterLostItems.salts_animal_farm.config.SaltsAnimalFarmConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SaltsAnimalFarmConfigLists {
    private static SaltsAnimalFarmConfig cachedConfig;
    private static EntityTypeMatcher farmAnimals;
    private static EntityTypeMatcher scaryMobs;
    private static BlockMatcher softBlocks;

    private SaltsAnimalFarmConfigLists() {
    }

    public static boolean isFarmAnimal(Entity entity) {
        if (!Salts_animal_farm.CONFIG.modEnabled()) {
            return false;
        }

        refreshIfNeeded();
        return farmAnimals.matches(entity);
    }

    public static boolean isScaryMob(Entity entity) {
        if (!Salts_animal_farm.CONFIG.modEnabled()) {
            return false;
        }

        refreshIfNeeded();
        return scaryMobs.matches(entity);
    }

    public static boolean isSoftBlock(BlockState state) {
        if (!Salts_animal_farm.CONFIG.modEnabled()) {
            return false;
        }

        refreshIfNeeded();
        return softBlocks.matches(state.getBlock());
    }

    private static void refreshIfNeeded() {
        SaltsAnimalFarmConfig config = Salts_animal_farm.CONFIG;

        if (config == cachedConfig && farmAnimals != null && scaryMobs != null && softBlocks != null) {
            return;
        }

        cachedConfig = config;
        farmAnimals = new EntityTypeMatcher(config.farmAnimals());
        scaryMobs = new EntityTypeMatcher(config.scaryMobs());
        softBlocks = new BlockMatcher(config.softBlocks());
    }

    private abstract static class RegistryListMatcher<T> {
        protected final Set<Identifier> ids = new HashSet<>();

        protected RegistryListMatcher(List<String> entries, ResourceKey<? extends net.minecraft.core.Registry<T>> registryKey) {
            for (String rawEntry : entries) {
                String entry = rawEntry.trim();

                try {
                    if (entry.startsWith("#")) {
                        addTag(TagKey.create(registryKey, Identifier.parse(entry.substring(1))));
                    } else {
                        ids.add(Identifier.parse(entry));
                    }
                } catch (RuntimeException exception) {
                    Salts_animal_farm.LOGGER.warn("Ignoring invalid config list entry '{}'", rawEntry);
                }
            }
        }

        protected abstract void addTag(TagKey<T> tag);
    }

    private static final class EntityTypeMatcher extends RegistryListMatcher<EntityType<?>> {
        private final Set<TagKey<EntityType<?>>> tags = new HashSet<>();

        private EntityTypeMatcher(List<String> entries) {
            super(entries, Registries.ENTITY_TYPE);
        }

        private boolean matches(Entity entity) {
            if (ids.contains(EntityType.getKey(entity.getType()))) {
                return true;
            }

            for (TagKey<EntityType<?>> tag : tags) {
                if (entity.getType().builtInRegistryHolder().is(tag)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        protected void addTag(TagKey<EntityType<?>> tag) {
            tags.add(tag);
        }
    }

    private static final class BlockMatcher extends RegistryListMatcher<Block> {
        private final Set<TagKey<Block>> tags = new HashSet<>();

        private BlockMatcher(List<String> entries) {
            super(entries, Registries.BLOCK);
        }

        private boolean matches(Block block) {
            if (ids.contains(BuiltInRegistries.BLOCK.getKey(block))) {
                return true;
            }

            for (TagKey<Block> tag : tags) {
                if (block.builtInRegistryHolder().is(tag)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        protected void addTag(TagKey<Block> tag) {
            tags.add(tag);
        }
    }
}
