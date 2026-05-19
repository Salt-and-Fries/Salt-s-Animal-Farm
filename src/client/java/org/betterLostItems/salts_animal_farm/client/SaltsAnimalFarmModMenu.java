package org.betterLostItems.salts_animal_farm.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import org.betterLostItems.salts_animal_farm.client.config.SaltsAnimalFarmConfigScreen;

public class SaltsAnimalFarmModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SaltsAnimalFarmConfigScreen::new;
    }
}
