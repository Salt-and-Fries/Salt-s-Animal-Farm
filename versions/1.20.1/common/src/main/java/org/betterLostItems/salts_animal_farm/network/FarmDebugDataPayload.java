package org.betterLostItems.salts_animal_farm.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;

import java.util.ArrayList;
import java.util.List;

public record FarmDebugDataPayload(List<Entry> entries) {
    private static final int MAX_ENTRIES = 128;
    private static final int MAX_LINES = 8;
    private static final int MAX_LINE_LENGTH = 256;
    public static final ResourceLocation ID = Salts_animal_farm.id("farm_debug_data");

    public static void encode(FarmDebugDataPayload payload, FriendlyByteBuf buffer) {
        buffer.writeVarInt(Math.min(payload.entries().size(), MAX_ENTRIES));

        for (int i = 0; i < payload.entries().size() && i < MAX_ENTRIES; i++) {
            Entry entry = payload.entries().get(i);
            buffer.writeVarInt(entry.entityId());
            buffer.writeVarInt(Math.min(entry.lines().size(), MAX_LINES));

            for (int lineIndex = 0; lineIndex < entry.lines().size() && lineIndex < MAX_LINES; lineIndex++) {
                buffer.writeUtf(entry.lines().get(lineIndex), MAX_LINE_LENGTH);
            }
        }
    }

    public static FarmDebugDataPayload decode(FriendlyByteBuf buffer) {
        int entryCount = Math.min(buffer.readVarInt(), MAX_ENTRIES);
        List<Entry> entries = new ArrayList<>(entryCount);

        for (int i = 0; i < entryCount; i++) {
            int entityId = buffer.readVarInt();
            int lineCount = Math.min(buffer.readVarInt(), MAX_LINES);
            List<String> lines = new ArrayList<>(lineCount);

            for (int lineIndex = 0; lineIndex < lineCount; lineIndex++) {
                lines.add(buffer.readUtf(MAX_LINE_LENGTH));
            }

            entries.add(new Entry(entityId, lines));
        }

        return new FarmDebugDataPayload(entries);
    }

    public record Entry(int entityId, List<String> lines) {
    }
}
