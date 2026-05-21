package org.betterLostItems.salts_animal_farm.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.betterLostItems.salts_animal_farm.Salts_animal_farm;
import org.betterLostItems.salts_animal_farm.client.AnimalFarmClientDebug;
import org.betterLostItems.salts_animal_farm.network.FarmAnimalStatePayload;
import org.betterLostItems.salts_animal_farm.network.FarmDebugDataPayload;
import org.betterLostItems.salts_animal_farm.network.OpenConfigScreenPayload;
import org.betterLostItems.salts_animal_farm.network.RenderDebugFarmDataPayload;
import org.betterLostItems.salts_animal_farm.network.SaltsAnimalFarmNetworking;

public final class SaltsAnimalFarmForgeNetworking {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            Salts_animal_farm.id("main"),
            () -> SaltsAnimalFarmNetworking.PROTOCOL_VERSION,
            SaltsAnimalFarmNetworking.PROTOCOL_VERSION::equals,
            SaltsAnimalFarmNetworking.PROTOCOL_VERSION::equals
    );
    private static boolean registered;

    private SaltsAnimalFarmForgeNetworking() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        int packetId = 0;
        CHANNEL.messageBuilder(OpenConfigScreenPayload.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenConfigScreenPayload::encode)
                .decoder(OpenConfigScreenPayload::decode)
                .consumerMainThread((payload, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> SaltsAnimalFarmForgeClientPackets::openConfigScreen);
                    context.get().setPacketHandled(true);
                })
                .add();
        CHANNEL.messageBuilder(RenderDebugFarmDataPayload.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RenderDebugFarmDataPayload::encode)
                .decoder(RenderDebugFarmDataPayload::decode)
                .consumerMainThread((payload, context) -> {
                    AnimalFarmClientDebug.setRenderDebugFarmData(payload.visible());
                    context.get().setPacketHandled(true);
                })
                .add();
        CHANNEL.messageBuilder(FarmDebugDataPayload.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FarmDebugDataPayload::encode)
                .decoder(FarmDebugDataPayload::decode)
                .consumerMainThread((payload, context) -> {
                    AnimalFarmClientDebug.setEntityDebugData(payload.entries());
                    context.get().setPacketHandled(true);
                })
                .add();
        CHANNEL.messageBuilder(FarmAnimalStatePayload.class, packetId, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FarmAnimalStatePayload::encode)
                .decoder(FarmAnimalStatePayload::decode)
                .consumerMainThread((payload, context) -> {
                    AnimalFarmClientDebug.setEntitySickStates(payload.entries());
                    context.get().setPacketHandled(true);
                })
                .add();

        registered = true;
    }

    public static boolean sendToPlayer(ServerPlayer player, Object payload) {
        if (payload instanceof OpenConfigScreenPayload
                || payload instanceof RenderDebugFarmDataPayload
                || payload instanceof FarmDebugDataPayload
                || payload instanceof FarmAnimalStatePayload) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
            return true;
        }

        return false;
    }
}
