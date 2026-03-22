package de.fisch37.betterserverpacksfabric.config_serializers;

import java.util.Optional;

/**
 * This evil exists to circumvent the limitation of Java's "generics".
 * MaybeType is an interface that describes types that perform the exact duty {@link Optional} has.
 * Implementors should be concrete (non-generic) types that implement the interface for one concrete type.
 * Where generics don't work, you can then use a known maybe type instead.
 * <p>
 * It is also recommended that every MaybeType have a static method converting from an optional to itself.
 * MaybeType cannot enforce this as statics cannot be abstract in Java.
 * @param <T> The type to wrap.
 * @see MaybeInstant
 */
public interface MaybeType<T> {
    Optional<T> asOptional();
}
