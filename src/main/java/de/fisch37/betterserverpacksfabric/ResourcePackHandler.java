package de.fisch37.betterserverpacksfabric;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.UUID;

public class ResourcePackHandler {
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> push(handler));
    }

    public static void push(ServerPlayNetworkHandler handler) {
        if (Main.getHash() != null) {
            final String url = Main.config.url.get();
            handler.sendPacket(new ResourcePackSendS2CPacket(
                    UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8)),
                    url,
                    Main.printHexBinary(Main.getHash()),
                    false,
                    java.util.Optional.empty()
            ));
        }
    }

    public static int pushTo(MinecraftServer server) {
        return pushTo(server.getPlayerManager());
    }
    public static int pushTo(PlayerManager players) {
        return pushTo(players.getPlayerList());
    }
    public static int pushTo(Collection<ServerPlayerEntity> players) {
        for (ServerPlayerEntity player : players) push(player.networkHandler);
        return players.size();
    }
}
