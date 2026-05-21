package org.betterLostItems.salts_animal_farm.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import org.betterLostItems.salts_animal_farm.client.config.SaltsAnimalFarmConfigScreen;

public final class SaltsAnimalFarmModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SaltsAnimalFarmConfigScreen::new;
    }
}
