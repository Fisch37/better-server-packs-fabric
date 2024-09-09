package de.fisch37.betterserverpacksfabric.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * Makes the best toast in the world
 */
public final class ToastHelper {
    private ToastHelper() { }

    public static void showToast(
            MinecraftClient client,
            Text title,
            @Nullable Text description
    ) {
        var toast = SystemToast.create(client, SystemToast.Type.WORLD_ACCESS_FAILURE, title, description);
        client.getToastManager().add(toast);
    }
}
