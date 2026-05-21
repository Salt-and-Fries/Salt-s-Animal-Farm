package org.betterLostItems.salts_animal_farm.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.betterLostItems.salts_animal_farm.client.AnimalFarmClientScreens;
import org.betterLostItems.salts_animal_farm.client.config.SaltsAnimalFarmConfigScreen;

public final class SaltsAnimalFarmNeoForgeClient {
    private SaltsAnimalFarmNeoForgeClient() {
    }

    public static void init(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (container, parent) -> new SaltsAnimalFarmConfigScreen(parent));
    }

    public static void openConfigScreen() {
        AnimalFarmClientScreens.openConfigScreen();
    }
}
