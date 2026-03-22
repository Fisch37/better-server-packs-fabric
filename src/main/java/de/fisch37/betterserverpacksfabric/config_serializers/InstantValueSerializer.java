package de.fisch37.betterserverpacksfabric.config_serializers;

import de.maxhenkel.configbuilder.entry.serializer.ValueSerializer;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * A serializer that can convert an {@link Instant} using a given formatter.
 */
public class InstantValueSerializer implements ValueSerializer<Instant> {
    public static final InstantValueSerializer ISO_INSTANT = new InstantValueSerializer(DateTimeFormatter.ISO_INSTANT);

    public final DateTimeFormatter formatter;

    public InstantValueSerializer(DateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public Instant deserialize(String str) {
        return Instant.from(formatter.parse(str));
    }

    @Override
    public String serialize(Instant val) {
        return formatter.format(val);
    }
}
