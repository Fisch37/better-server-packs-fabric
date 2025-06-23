package de.fisch37.betterserverpacksfabric.client.gui;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;

public class InputHandler {
    private static final String CATEGORY = "key.categories.bsp";
    public static KeyBinding openConfigScreen;

    public static void initialise() {
        openConfigScreen = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bsp.open_config",
                80,
                CATEGORY
        ));
    }
}
