package org.betterLostItems.salts_animal_farm.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;

import java.util.ArrayList;
import java.util.List;

public record FarmAnimalStatePayload(List<Entry> entries) implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 256;
    public static final Type<FarmAnimalStatePayload> TYPE = new Type<>(Salts_animal_farm.id("farm_animal_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FarmAnimalStatePayload> CODEC = StreamCodec.of(
            FarmAnimalStatePayload::encode,
            FarmAnimalStatePayload::decode
    );

    private static void encode(RegistryFriendlyByteBuf buffer, FarmAnimalStatePayload payload) {
        ByteBufCodecs.VAR_INT.encode(buffer, Math.min(payload.entries.size(), MAX_ENTRIES));

        for (int i = 0; i < payload.entries.size() && i < MAX_ENTRIES; i++) {
            Entry entry = payload.entries.get(i);
            ByteBufCodecs.VAR_INT.encode(buffer, entry.entityId());
            buffer.writeBoolean(entry.sick());
        }
    }

    private static FarmAnimalStatePayload decode(RegistryFriendlyByteBuf buffer) {
        int entryCount = Math.min(ByteBufCodecs.VAR_INT.decode(buffer), MAX_ENTRIES);
        List<Entry> entries = new ArrayList<>(entryCount);

        for (int i = 0; i < entryCount; i++) {
            entries.add(new Entry(ByteBufCodecs.VAR_INT.decode(buffer), buffer.readBoolean()));
        }

        return new FarmAnimalStatePayload(entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(int entityId, boolean sick) {
    }
}
