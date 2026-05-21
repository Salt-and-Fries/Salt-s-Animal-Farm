package org.betterLostItems.salts_animal_farm.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;

public record RenderDebugFarmDataPayload(boolean visible) implements CustomPacketPayload {
    public static final Type<RenderDebugFarmDataPayload> TYPE = new Type<>(Salts_animal_farm.id("render_debug_farm_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RenderDebugFarmDataPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            RenderDebugFarmDataPayload::visible,
            RenderDebugFarmDataPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
