package de.fisch37.betterserverpacksfabric;

import de.maxhenkel.configbuilder.ConfigBuilder;
import de.maxhenkel.configbuilder.entry.ConfigEntry;

public class Config {
    public ConfigEntry<String> url;
    public ConfigEntry<Boolean> rehashOnStart;

    public Config(ConfigBuilder builder) {
        url = builder.stringEntry("url", "");
        rehashOnStart = builder.booleanEntry("rehash_on_start", false);
    }
}
