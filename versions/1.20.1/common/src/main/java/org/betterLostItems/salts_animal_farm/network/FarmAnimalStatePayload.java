package org.betterLostItems.salts_animal_farm.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;

import java.util.ArrayList;
import java.util.List;

public record FarmAnimalStatePayload(List<Entry> entries) {
    private static final int MAX_ENTRIES = 256;
    public static final ResourceLocation ID = Salts_animal_farm.id("farm_animal_state");

    public static void encode(FarmAnimalStatePayload payload, FriendlyByteBuf buffer) {
        buffer.writeVarInt(Math.min(payload.entries().size(), MAX_ENTRIES));

        for (int i = 0; i < payload.entries().size() && i < MAX_ENTRIES; i++) {
            Entry entry = payload.entries().get(i);
            buffer.writeVarInt(entry.entityId());
            buffer.writeBoolean(entry.sick());
        }
    }

    public static FarmAnimalStatePayload decode(FriendlyByteBuf buffer) {
        int entryCount = Math.min(buffer.readVarInt(), MAX_ENTRIES);
        List<Entry> entries = new ArrayList<>(entryCount);

        for (int i = 0; i < entryCount; i++) {
            entries.add(new Entry(buffer.readVarInt(), buffer.readBoolean()));
        }

        return new FarmAnimalStatePayload(entries);
    }

    public record Entry(int entityId, boolean sick) {
    }
}
