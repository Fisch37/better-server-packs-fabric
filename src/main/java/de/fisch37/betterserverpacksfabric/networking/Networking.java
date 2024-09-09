package de.fisch37.betterserverpacksfabric.networking;

import de.fisch37.betterserverpacksfabric.ServerMain;
import io.wispforest.owo.network.OwoNetChannel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import static de.fisch37.betterserverpacksfabric.Main.MOD_ID;
import static de.fisch37.betterserverpacksfabric.ServerMain.config;

public class Networking {
    public static final OwoNetChannel CHANNEL = OwoNetChannel.create(Identifier.of(MOD_ID, "main"));

    public static void registerAll() {
        CHANNEL.registerClientboundDeferred(CanChangeConfigPacket.class);
        CHANNEL.registerClientboundDeferred(PackState.class);
        CHANNEL.registerServerbound(PackState.class, (packet, access) -> {
            if (ServerMain.hasConfigAccess(access.player())) {
                config.url.set(packet.url()).save();
                config.required.set(packet.required()).save();
                config.rehashOnStart.set(packet.rehashNextStart()).save();
                config.setPrompt(
                        packet.prompt().orElse(null),
                        access.player().getRegistryManager()
                ).save();
                sendConfigUpdate(access.runtime());
            }
        });
    }

    public static void sendConfigUpdate(MinecraftServer server) {
        var targets = server.getPlayerManager()
                .getPlayerList()
                .stream()
                .filter(ServerMain::hasConfigAccess)
                .toList();
        CHANNEL.serverHandle(targets).send(PackState.fromConfig(config, server.getRegistryManager()));
    }
}
