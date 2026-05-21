package org.betterLostItems.salts_animal_farm.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;

public record RenderDebugFarmDataPayload(boolean visible) {
    public static final ResourceLocation ID = Salts_animal_farm.id("render_debug_farm_data");

    public static void encode(RenderDebugFarmDataPayload payload, FriendlyByteBuf buffer) {
        buffer.writeBoolean(payload.visible());
    }

    public static RenderDebugFarmDataPayload decode(FriendlyByteBuf buffer) {
        return new RenderDebugFarmDataPayload(buffer.readBoolean());
    }
}
