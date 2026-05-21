package org.betterLostItems.salts_animal_farm.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;

public record OpenConfigScreenPayload() implements CustomPacketPayload {
    public static final Type<OpenConfigScreenPayload> TYPE = new Type<>(Salts_animal_farm.id("open_config_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenConfigScreenPayload> CODEC = StreamCodec.unit(new OpenConfigScreenPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
