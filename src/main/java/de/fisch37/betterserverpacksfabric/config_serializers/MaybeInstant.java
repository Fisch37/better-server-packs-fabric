package de.fisch37.betterserverpacksfabric.config_serializers;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import java.time.Instant;
import java.util.Optional;

@NullMarked
public record MaybeInstant(@Nullable Instant inst) implements MaybeType<Instant> {
    @Override
    public Optional<Instant> asOptional() {
        return Optional.ofNullable(inst);
    }

    public static MaybeInstant empty() {
        return new MaybeInstant(null);
    }

    public static MaybeInstant of(Instant inst) {
        return new MaybeInstant(inst);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static MaybeInstant fromOpt(Optional<Instant> inst) {
        return new MaybeInstant(inst.orElse(null));
    }
}
