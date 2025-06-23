package de.fisch37.betterserverpacksfabric;

import de.fisch37.betterserverpacksfabric.networking.Networking;
import net.fabricmc.api.ModInitializer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HexFormat;

public class Main implements ModInitializer {
    public static final String MOD_ID = "betterserverpacks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * Runs the mod initializer for common operations.
     */
    @Override
    public void onInitialize() {
        Networking.registerAll();
    }

    private static final HexFormat HEX_FORMAT = HexFormat.of();

    public static String printHexBinary(byte @NotNull[] val) {
        return HEX_FORMAT.formatHex(val);
    }

    public static byte[] readHexBinary(@NotNull String val) {
        return HEX_FORMAT.parseHex(val);
    }
}
