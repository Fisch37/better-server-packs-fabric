package de.fisch37.betterserverpacksfabric.networking;

import de.fisch37.betterserverpacksfabric.Config;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Contract;

import java.util.Optional;

public record PackState(
        String url,
        boolean required,
        boolean rehashNextStart,
        Optional<Text> prompt
) {
    @Contract(value = "_, _ -> new")
    public static PackState fromConfig(Config config, RegistryWrapper.WrapperLookup registryLookup) {
        return new PackState(
                config.url.get(),
                config.required.get(),
                config.rehashOnStart.get(),
                config.getPrompt(registryLookup)
        );
    }
}
