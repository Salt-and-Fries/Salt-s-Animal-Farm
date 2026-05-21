package org.betterLostItems.salts_animal_farm.neoforge;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import org.betterLostItems.salts_animal_farm.client.config.SaltsAnimalFarmConfigScreen;

public final class SaltsAnimalFarmNeoForgeClient {
    private SaltsAnimalFarmNeoForgeClient() {
    }

    public static void init(IEventBus modBus) {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new SaltsAnimalFarmConfigScreen(parent))
        );
    }
}
