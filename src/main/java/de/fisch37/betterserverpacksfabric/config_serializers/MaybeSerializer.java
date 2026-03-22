package de.fisch37.betterserverpacksfabric.config_serializers;

import de.maxhenkel.configbuilder.entry.serializer.ValueSerializer;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Wraps another serializer to support optional values
 * by declaring a certain string as the empty value.
 * Usually this is the empty string.
 *
 * @param <T> The serialization target.
 * @implNote The <em>emptyValue</em> should not a valid result of <em>inner::serialize</em>.
 * If the inner serializer returns the empty value, that value will be read back as {@code Optional.empty()}!
 */
@NullMarked
public record MaybeSerializer<T, M extends MaybeType<T>>(
        ValueSerializer<T> inner, String emptyValue, Function<Optional<T>, M> maybeFromOpt
) implements ValueSerializer<M> {
    private Optional<T> deserializeInner(String str) {
        if (str.equals(this.emptyValue)) {
            return Optional.empty();
        } else {
            return Optional.of(Objects.requireNonNull(inner.deserialize(str)));
        }
    }

    @Override
    public M deserialize(String str) {
        return maybeFromOpt.apply(deserializeInner(str));
    }

    @Override
    public String serialize(M val) {
        return val.asOptional()
                .map(inner::serialize)
                .orElse(this.emptyValue);
    }

    @Contract("_, _ -> new")
    public static <T, M extends MaybeType<T>> MaybeSerializer<T, M> normallyEmpty(
            ValueSerializer<T> inner,
            Function<Optional<T>, M> maybeFromOpt
    ) {
        return new MaybeSerializer<>(inner, "", maybeFromOpt);
    }
}
