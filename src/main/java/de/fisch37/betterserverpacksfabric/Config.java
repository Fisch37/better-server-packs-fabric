package de.fisch37.betterserverpacksfabric;

import de.maxhenkel.configbuilder.ConfigBuilder;
import de.maxhenkel.configbuilder.entry.ConfigEntry;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static de.fisch37.betterserverpacksfabric.Main.LOGGER;

public class Config {
    public ConfigEntry<String> url;
    public ConfigEntry<Boolean> rehashOnStart;
    public ConfigEntry<Boolean> required;
    public ConfigEntry<String> prompt;

    public Config(ConfigBuilder builder) {
        url = builder.stringEntry("url", "");
        rehashOnStart = builder.booleanEntry("rehash_on_start", false);
        required = builder.booleanEntry("required", false);
        prompt = builder.stringEntry("prompt", "");
    }

    public Optional<Text> getPrompt(@NotNull RegistryWrapper.WrapperLookup registries) {
        String promptString = this.prompt.get();
        if (promptString.isBlank()) {
            return Optional.empty();
        } else {
            try {
                // This nesting is a bit scuffed, but I think it's the best solution
                return Optional.of(
                        Optional.ofNullable(Text.Serialization.fromJson(promptString, registries))
                                .orElseThrow()
                );
            } catch (Exception e) {
                // Need to use concatenation to log the exception
                LOGGER.error("Failed to parse prompt text " + promptString, e);
                return Optional.empty();
            }
        }
    }
}
