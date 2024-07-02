package de.fisch37.betterserverpacksfabric;

import de.maxhenkel.configbuilder.ConfigBuilder;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static de.fisch37.betterserverpacksfabric.PackCommand.MSG_PREFIX;


public class Main implements DedicatedServerModInitializer {
    public static final String MOD_ID = "betterserverpacks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final byte SHA1_HASH_SIZE = 20;

    public static Config config;
    private static byte @Nullable [] hash;
    /**
     * Runs the mod initializer on the server environment.
     */
    @Override
    public void onInitializeServer() {
        config = ConfigBuilder.builder(Config::new)
                .path(getModConfigFile())
                .strict(true)
                .saveAfterBuild(true)
                .build();
        readHash();
        if (config.rehashOnStart.get()) {
            ServerLifecycleEvents.SERVER_STARTED.register(server -> {
                updateHash(server, null, false);
                config.rehashOnStart.set(false).save();
            });
        }

        PackCommand.register();
        ResourcePackHandler.register();
    }

    public static Path getModConfigFolder() {
        return Path.of(".").resolve("config").resolve(MOD_ID);
    }
    public static Path getModConfigFile() {
        return getModConfigFolder().resolve("config.properties");
    }
    public static File getHashFile() {
        return getModConfigFolder().resolve("pack.sha1").toFile();
    }

    public static byte @Nullable [] getHash() {
        return hash;
    }

    private static void saveHash() {
        if (hash == null) {
            getHashFile().delete();
        } else {
            try (FileWriter file = new FileWriter(getHashFile())) {
                file.write(printHexBinary(hash));
            } catch (IOException e) {
                LOGGER.error("Failed to write pack hash to file. Hash update will not carry over restarts.");
            }
        }
    }

    private static void readHash() {
        String newHashHex;
        if (config.url.get().isEmpty()) {
            hash = null;
            return;
        }
        try (BufferedReader file = new BufferedReader(new FileReader(getHashFile()))) {
            newHashHex = file.readLine();
        } catch (FileNotFoundException e) {
            LOGGER.error("No hash file found, you will need to execute /pack reload to apply");
            hash = null;
            return;
        } catch (IOException e) {
            LOGGER.error("Failed to read hash: IOException");
            hash = null;
            return;
        }
        if (newHashHex.length() != SHA1_HASH_SIZE * 2) {
            LOGGER.error("Pack hash had incorrect length. Assuming false value");
            // Writing old hash to file as correction attempt
            saveHash();
            return;
        }

        hash = readHexBinary(newHashHex);
    }

    public static void updateHash(
            MinecraftServer server,
            @Nullable ServerCommandSource source,
            boolean pushAfterSet
    ) throws IllegalStateException {
        // TODO: Segregate the feedback somehow
        if (config.url.get().isEmpty()){
            hash = null;
            if (source != null) {
                source.sendFeedback(() -> MSG_PREFIX.copy()
                                .append("BetterServerPacks has been disabled. ")
                                .append("This cannot be pushed to the players :(")
                        ,
                        true
                );
            }
        }
        else {
            URL url;
            try {
                url = new URI(config.url.get()).toURL();
            } catch (MalformedURLException | URISyntaxException e) {
                throw new IllegalStateException("Pack URL has invalid format", e);
            }
            server.execute(() -> {
                MessageDigest digest;
                try {
                    digest = MessageDigest.getInstance("SHA-1");
                } catch (NoSuchAlgorithmException e) {
                    // SHA-1 is required per java docs
                    LOGGER.error("JVM does not have SHA-1 hashing... WTF?");
                    return;
                }
                try (InputStream data = url.openStream()) {
                    new DigestInputStream(data, digest).readAllBytes();
                } catch (IOException e) {
                    LOGGER.error("Failed to load resource pack at {}", url);
                    return;
                }
                hash = digest.digest();
                Main.saveHash();
                if (pushAfterSet) ResourcePackHandler.pushTo(server);

                if (source != null) {
                    source.sendFeedback(() -> MSG_PREFIX.copy()
                                    .append("Pack Hash has been updated!")
                            ,
                            true);
                }
            });
        }
    }


    private static final HexFormat HEX_FORMAT = HexFormat.of();

    public static String printHexBinary(byte @NotNull[] val) {
        return HEX_FORMAT.formatHex(val);
    }

    public static byte[] readHexBinary(@NotNull String val) {
        return HEX_FORMAT.parseHex(val);
    }
}
