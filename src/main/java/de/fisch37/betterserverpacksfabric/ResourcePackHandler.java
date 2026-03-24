package de.fisch37.betterserverpacksfabric;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.UUID;

public class ResourcePackHandler {
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> push(handler));
    }

    public static void push(ServerGamePacketListenerImpl handler) {
        if (Main.getHash() != null) {
            final String url = Main.config.url.get();
            handler.send(new ClientboundResourcePackPushPacket(
                    UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8)),
                    url,
                    Main.printHexBinary(Main.getHash()),
                    Main.config.required.get(),
                    Main.config.getPrompt(handler.player.registryAccess()))
            );
        }
    }

    public static int pushTo(MinecraftServer server) {
        return pushTo(server.getPlayerList());
    }
    public static int pushTo(PlayerList players) {
        return pushTo(players.getPlayers());
    }
    public static int pushTo(Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) push(player.connection);
        return players.size();
    }
}
