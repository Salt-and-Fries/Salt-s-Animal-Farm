package org.betterLostItems.salts_animal_farm.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;

import java.util.ArrayList;
import java.util.List;

public record FarmDebugDataPayload(List<Entry> entries) implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 128;
    private static final int MAX_LINES = 8;
    public static final Type<FarmDebugDataPayload> TYPE = new Type<>(Salts_animal_farm.id("farm_debug_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FarmDebugDataPayload> CODEC = StreamCodec.of(
            FarmDebugDataPayload::encode,
            FarmDebugDataPayload::decode
    );

    private static void encode(RegistryFriendlyByteBuf buffer, FarmDebugDataPayload payload) {
        ByteBufCodecs.VAR_INT.encode(buffer, Math.min(payload.entries.size(), MAX_ENTRIES));

        for (int i = 0; i < payload.entries.size() && i < MAX_ENTRIES; i++) {
            Entry entry = payload.entries.get(i);
            ByteBufCodecs.VAR_INT.encode(buffer, entry.entityId());
            ByteBufCodecs.VAR_INT.encode(buffer, Math.min(entry.lines().size(), MAX_LINES));

            for (int lineIndex = 0; lineIndex < entry.lines().size() && lineIndex < MAX_LINES; lineIndex++) {
                ByteBufCodecs.STRING_UTF8.encode(buffer, entry.lines().get(lineIndex));
            }
        }
    }

    private static FarmDebugDataPayload decode(RegistryFriendlyByteBuf buffer) {
        int entryCount = Math.min(ByteBufCodecs.VAR_INT.decode(buffer), MAX_ENTRIES);
        List<Entry> entries = new ArrayList<>(entryCount);

        for (int i = 0; i < entryCount; i++) {
            int entityId = ByteBufCodecs.VAR_INT.decode(buffer);
            int lineCount = Math.min(ByteBufCodecs.VAR_INT.decode(buffer), MAX_LINES);
            List<String> lines = new ArrayList<>(lineCount);

            for (int lineIndex = 0; lineIndex < lineCount; lineIndex++) {
                lines.add(ByteBufCodecs.STRING_UTF8.decode(buffer));
            }

            entries.add(new Entry(entityId, lines));
        }

        return new FarmDebugDataPayload(entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(int entityId, List<String> lines) {
    }
}
