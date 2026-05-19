package org.betterLostItems.salts_animal_farm;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.betterLostItems.salts_animal_farm.config.SaltsAnimalFarmConfig;
import org.betterLostItems.salts_animal_farm.debug.AnimalFarmDebugDataSender;
import org.betterLostItems.salts_animal_farm.debug.AnimalFarmDebugCommands;
import org.betterLostItems.salts_animal_farm.entity.FarmAnimalFearHandler;
import org.betterLostItems.salts_animal_farm.network.SaltsAnimalFarmNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Salts_animal_farm implements ModInitializer {
    public static final String MOD_ID = "salts_animal_farm";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static SaltsAnimalFarmConfig CONFIG = SaltsAnimalFarmConfig.DEFAULT;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        CONFIG = SaltsAnimalFarmConfig.load();
        SaltsAnimalFarmNetworking.registerPayloads();

        if (!CONFIG.modEnabled()) {
            LOGGER.info("Salt's Animal Farm is disabled by config");
            return;
        }

        AnimalFarmDebugDataSender.register();
        AnimalFarmDebugCommands.register();
        FarmAnimalFearHandler.register();
    }
}
