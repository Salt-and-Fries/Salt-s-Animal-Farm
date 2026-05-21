package org.betterLostItems.salts_animal_farm.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;

public record OpenConfigScreenPayload() {
    public static final ResourceLocation ID = Salts_animal_farm.id("open_config_screen");

    public static void encode(OpenConfigScreenPayload payload, FriendlyByteBuf buffer) {
    }

    public static OpenConfigScreenPayload decode(FriendlyByteBuf buffer) {
        return new OpenConfigScreenPayload();
    }
}
