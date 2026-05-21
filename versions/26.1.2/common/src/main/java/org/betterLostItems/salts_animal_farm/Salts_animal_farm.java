package org.betterLostItems.salts_animal_farm;

import net.minecraft.resources.Identifier;
import org.betterLostItems.salts_animal_farm.config.SaltsAnimalFarmConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public final class Salts_animal_farm {
    public static final String MOD_ID = "salts_animal_farm";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static volatile SaltsAnimalFarmConfig CONFIG = SaltsAnimalFarmConfig.DEFAULT;

    private Salts_animal_farm() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void init(Path configDir) {
        SaltsAnimalFarmConfig.configure(configDir);
        CONFIG = SaltsAnimalFarmConfig.load();

        if (!CONFIG.modEnabled()) {
            LOGGER.info("Salt's Animal Farm is disabled by config");
        }
    }

    public static void updateConfig(SaltsAnimalFarmConfig config) {
        CONFIG = config.sanitized();
        SaltsAnimalFarmConfig.save(CONFIG);
    }
}
