package de.fisch37.betterserverpacksfabric.client;

import de.fisch37.betterserverpacksfabric.client.gui.InputHandler;
import de.fisch37.betterserverpacksfabric.client.gui.ServerConfigScreen;
import de.fisch37.betterserverpacksfabric.networking.CanChangeConfigPacket;
import de.fisch37.betterserverpacksfabric.networking.Networking;
import de.fisch37.betterserverpacksfabric.networking.PackState;
import io.wispforest.owo.util.Observable;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class MainClient implements ClientModInitializer {
    private static Observable<@Nullable Boolean> canChangeConfig = Observable.of(null);
    private static final Observable<@Nullable PackState> packState = Observable.of(null);
    private static boolean isConnected = false;

    @Override
    public void onInitializeClient() {
        InputHandler.initialise();
        Networking.CHANNEL.registerClientbound(
                CanChangeConfigPacket.class,
                (packet, access) -> canChangeConfig.set(packet.canChangeConfig())
        );
        Networking.CHANNEL.registerClientbound(
                PackState.class,
                (packet, access) -> {}
        );
        ClientPlayConnectionEvents.JOIN.register(((handler, sender, client) -> isConnected = true));
        ClientPlayConnectionEvents.DISCONNECT.register(((handler, client) -> {
            canChangeConfig = null;
            isConnected = false;
        }));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (InputHandler.openConfigScreen.wasPressed() && isConnected) {
                final var canChangeConfigVal = canChangeConfig.get();
                if (canChangeConfigVal == null) {
                    ToastHelper.showToast(
                            client,
                            Text.translatable("bsp.toast.race_permission"),
                            Text.translatable("bsp.toast.race_permission.description")
                    );
                }
                else if (canChangeConfigVal){
                    if (packState.get() == null) {
                        ToastHelper.showToast(
                                client,
                                Text.translatable("bsp.toast.race_pack"),
                                Text.translatable("bsp.toas.race_pack.description")
                        );
                    }
                    else client.setScreen(new ServerConfigScreen(packState));
                } else {
                    ToastHelper.showToast(
                            client,
                            Text.translatable("bsp.toast.no_permission"),
                            Text.translatable("bsp.toast.no_permission.description")
                    );
                }
            }
        });
    }

    public static Observable<Boolean> getCanChangeConfig() {
        return canChangeConfig;
    }

    public static boolean isConnected() {
        return isConnected;
    }
}
