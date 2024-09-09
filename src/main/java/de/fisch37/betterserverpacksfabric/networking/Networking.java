package de.fisch37.betterserverpacksfabric.networking;

import com.mojang.authlib.GameProfile;
import de.fisch37.betterserverpacksfabric.ServerMain;
import io.wispforest.owo.network.OwoNetChannel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.List;

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

    public static void sendConfigUpdate(Collection<ServerPlayerEntity> targets) {
        CHANNEL.serverHandle(targets).send(PackState.fromConfig(
                config,
                targets.iterator().next().getRegistryManager()
        ));
    }
    public static void sendConfigUpdate(MinecraftServer server) {
        sendConfigUpdate(server.getPlayerManager().getPlayerList()
                .stream()
                .filter(ServerMain::hasConfigAccess)
                .toList()
        );
    }

    public static void sendAccessUpdate(ServerPlayerEntity player) {
        final var hasAccess = ServerMain.hasConfigAccess(player);
        Networking.CHANNEL.serverHandle(player)
                .send(new CanChangeConfigPacket(hasAccess));
        if (hasAccess) {
            sendConfigUpdate(List.of(player));
        }
    }

    public static void sendAccessUpdate(MinecraftServer server, Collection<GameProfile> targets) {
        targets.forEach(target -> sendAccessUpdate(server.getPlayerManager().getPlayer(target.getId())));
    }
}
