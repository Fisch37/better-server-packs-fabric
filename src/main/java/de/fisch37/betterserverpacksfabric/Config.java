package de.fisch37.betterserverpacksfabric;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DynamicOps;
import de.fisch37.betterserverpacksfabric.config_serializers.MaybeInstant;
import de.maxhenkel.configbuilder.ConfigBuilder;
import de.maxhenkel.configbuilder.entry.ConfigEntry;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtGrammar;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.parsing.packrat.commands.CommandArgumentParser;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static de.fisch37.betterserverpacksfabric.Main.LOGGER;

public class Config {
    public final ConfigEntry<String> url;
    public final ConfigEntry<Boolean> rehashOnStart;
    public final ConfigEntry<Boolean> required;
    public final ConfigEntry<String> prompt;
    public final ConfigEntry<MaybeInstant> lastPolled;

    public Config(ConfigBuilder builder) {
        url = builder.stringEntry("url", "");
        rehashOnStart = builder.booleanEntry("rehash_on_start", false);
        required = builder.booleanEntry("required", false);
        prompt = builder.stringEntry("prompt", "");
        lastPolled = builder.entry("last_hash_update", MaybeInstant.empty());
    }

    public Optional<Component> getPrompt(@NotNull HolderLookup.Provider registries) {
        String promptString = this.prompt.get();
        if (promptString.isBlank()) {
            return Optional.empty();
        } else {
            try {
                return Optional.of(textFromSnbt(promptString, registries));
            } catch (Exception e) {
                // Need to use concatenation to log the exception
                LOGGER.error("Failed to parse prompt text " + promptString, e);
                return Optional.empty();
            }
        }
    }

    @Contract("null, null -> _; !null, !null -> _")
    public ConfigEntry<String> setPrompt(@Nullable Component prompt, @Nullable HolderLookup.Provider registries) {
        if (prompt == null) {
            this.prompt.set("");
        } else {
            assert registries != null; // See the contract
            this.prompt.set(textToSnbt(prompt, registries));
        }
        return this.prompt;
    }

    private static final DynamicOps<Tag> OPS = NbtOps.INSTANCE;
    private static final CommandArgumentParser<Tag> PARSER = SnbtGrammar.createParser(OPS);

    private static Component textFromSnbt(String snbt, @Nullable HolderLookup.Provider registries)
            throws CommandSyntaxException {
        final var reader = new StringReader(snbt);
        var ops = registries == null ? OPS : registries.createSerializationContext(OPS);
        return PARSER.withCodec(ops, PARSER, ComponentSerialization.CODEC, ComponentArgument.ERROR_INVALID_COMPONENT)
                .parseForCommands(reader);
    }

    private static String textToSnbt(Component text, HolderLookup.Provider registries)
            throws IllegalStateException {
        var element = ComponentSerialization.CODEC.encodeStart(registries.createSerializationContext(OPS), text).getOrThrow();
        // This just feels wrong, but it seems this is the correct way.
        return element.toString();
    }
}
