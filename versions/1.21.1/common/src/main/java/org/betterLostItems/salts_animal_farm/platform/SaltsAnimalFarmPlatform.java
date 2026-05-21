package org.betterLostItems.salts_animal_farm.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class SaltsAnimalFarmPlatform {
    private static ClientboundPacketSender clientboundPacketSender = (player, payload) -> false;

    private SaltsAnimalFarmPlatform() {
    }

    public static void setClientboundPacketSender(ClientboundPacketSender sender) {
        clientboundPacketSender = sender == null ? (player, payload) -> false : sender;
    }

    public static boolean trySend(ServerPlayer player, CustomPacketPayload payload) {
        return clientboundPacketSender.trySend(player, payload);
    }

    @FunctionalInterface
    public interface ClientboundPacketSender {
        boolean trySend(ServerPlayer player, CustomPacketPayload payload);
    }
}
