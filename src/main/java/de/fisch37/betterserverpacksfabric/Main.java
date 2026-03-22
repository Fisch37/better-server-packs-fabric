package de.fisch37.betterserverpacksfabric;

import de.fisch37.betterserverpacksfabric.config_serializers.InstantValueSerializer;
import de.fisch37.betterserverpacksfabric.config_serializers.MaybeInstant;
import de.fisch37.betterserverpacksfabric.config_serializers.MaybeSerializer;
import de.maxhenkel.configbuilder.ConfigBuilder;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.fisch37.betterserverpacksfabric.pack_downloaders.HttpDownloader;
import de.fisch37.betterserverpacksfabric.pack_downloaders.PackDownloader;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class Main implements DedicatedServerModInitializer {
    public static final String MOD_ID = "betterserverpacks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final byte SHA1_HASH_SIZE = 20;
    private static PackDownloader downloader;

    public static Config config;
    private static byte @Nullable [] hash;
    /**
     * Runs the mod initializer on the server environment.
     */
    @Override
    public void onInitializeServer() {
        config = ConfigBuilder.builder(Config::new)
                .addValueSerializer(
                        MaybeInstant.class,
                        MaybeSerializer.normallyEmpty(
                                InstantValueSerializer.ISO_INSTANT,
                                MaybeInstant::fromOpt
                        ))
                .path(getModConfigFile())
                .strict(true)
                .saveAfterBuild(true)
                .build();
        downloader = new HttpDownloader();
        readHash();
        if (config.rehashOnStart.get()) {
            doRehashOnStart();
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

    public static @Nullable String getHashString() {
        byte[] hash = getHash();
        return hash == null ? null : printHexBinary(hash);
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

    private static void doRehashOnStart() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("Updating pack hash...");
            updateHash().ifPresentOrElse(
                    future -> future.thenAccept(state ->
                        LOGGER.info("Pack hash updated!")
                    ),
                    () -> LOGGER.info("No pack is set. Cannot hash it")
            );
        });
    }

    /**
     * Starts a process to update the hash of the current pack,
     * if one is set and returns a future to that state.
     * The result of this operation will be logged to console.
     *
     * @return A future to the running updating process,
     *  if a pack was set, else an empty optional.
     *  The future completes with the new pack info
     *  or an exception if one was encountered.
     * @throws IllegalStateException if the current URL is malformed
     */
    public static Optional<CompletableFuture<PackDownloader.@Nullable PackState>> updateHash() throws IllegalStateException {
        if (config.url.get().isEmpty()){
            return Optional.empty();
        }

        URL url;
        try {
            url = new URI(config.url.get()).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new IllegalStateException("Pack URL has invalid format", e);
        }

        return Optional.of(
                downloader.getHashInThread(
                    url,
                    config.lastPolled.get().asOptional().orElse(null)
                ).whenComplete((packState, exc) -> {
                    if (packState != null) {
                        hash = packState.hash();
                        config.lastPolled
                                .set(MaybeInstant.of(packState.polledAt()))
                                .save();
                        saveHash();
                    }
                    if (exc != null) {
                        LOGGER.error("Error while trying to update pack hash", exc);
                    }
                })
        );
    }


    private static final HexFormat HEX_FORMAT = HexFormat.of();

    public static String printHexBinary(byte @NotNull[] val) {
        return HEX_FORMAT.formatHex(val);
    }

    public static byte[] readHexBinary(@NotNull String val) {
        return HEX_FORMAT.parseHex(val);
    }
}
