package de.fisch37.betterserverpacksfabric;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DynamicOps;
import de.fisch37.betterserverpacksfabric.config_serializers.MaybeInstant;
import de.maxhenkel.configbuilder.ConfigBuilder;
import de.maxhenkel.configbuilder.entry.ConfigEntry;
import net.minecraft.command.argument.TextArgumentType;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtParsing;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.packrat.Parser;
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

    public Optional<Text> getPrompt(@NotNull RegistryWrapper.WrapperLookup registries) {
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
    public ConfigEntry<String> setPrompt(@Nullable Text prompt, @Nullable RegistryWrapper.WrapperLookup registries) {
        if (prompt == null) {
            this.prompt.set("");
        } else {
            assert registries != null; // See the contract
            this.prompt.set(textToSnbt(prompt, registries));
        }
        return this.prompt;
    }

    private static final DynamicOps<NbtElement> OPS = NbtOps.INSTANCE;
    private static final Parser<NbtElement> PARSER = SnbtParsing.createParser(OPS);

    private static Text textFromSnbt(String snbt, @Nullable RegistryWrapper.WrapperLookup registries)
            throws CommandSyntaxException {
        final var reader = new StringReader(snbt);
        var ops = registries == null ? OPS : registries.getOps(OPS);
        return PARSER.withDecoding(ops, PARSER, TextCodecs.CODEC, TextArgumentType.INVALID_COMPONENT_EXCEPTION)
                .parse(reader);
    }

    private static String textToSnbt(Text text, RegistryWrapper.WrapperLookup registries)
            throws IllegalStateException {
        var element = TextCodecs.CODEC.encodeStart(registries.getOps(OPS), text).getOrThrow();
        // This just feels wrong, but it seems this is the correct way.
        return element.toString();
    }
}
