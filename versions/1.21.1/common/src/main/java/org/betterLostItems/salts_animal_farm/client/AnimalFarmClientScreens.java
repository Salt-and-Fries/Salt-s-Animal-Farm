package org.betterLostItems.salts_animal_farm.client;

import net.minecraft.client.Minecraft;
import org.betterLostItems.salts_animal_farm.client.config.SaltsAnimalFarmConfigScreen;

public final class AnimalFarmClientScreens {
    private AnimalFarmClientScreens() {
    }

    public static void openConfigScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new SaltsAnimalFarmConfigScreen(minecraft.screen));
    }
}
