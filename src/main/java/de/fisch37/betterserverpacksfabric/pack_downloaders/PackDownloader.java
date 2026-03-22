package de.fisch37.betterserverpacksfabric.pack_downloaders;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@NullMarked
public interface PackDownloader {
    /**
     * Recalculates the hash for the given pack data
     * @param url The url of the target pack
     * @param lastPolled The point in time when this pack was last polled.
     *                   Null if the pack has not been polled before.
     * @return A byte array containing the new hash, or null, if the hash hasn't changed since <em>lastPolled</em>.
     * @implSpec This function must not return null if <em>lastPolled</em> was null.
     */
    @Contract(value = "_,null -> !null")
    byte @Nullable [] getHash(URL url, @Nullable Instant lastPolled) throws IOException;

    default CompletableFuture<@Nullable PackState> getHashInThread(URL url, @Nullable Instant lastPolled) {
        var future = new CompletableFuture<@Nullable PackState>();
        // Ooo, threading in Minecraft code!
        // It's fine though, as long as we don't touch any of Minecraft's stuff
        new Thread(() -> {
            // Wonderfully huge and broad exception handler
            // (passes the exception into the CompletableFuture)
            try {
                // eager poll is necessary because if we save the time after receiving the response,
                // we may miss an update (race condition between server and client).
                var pollTime = Instant.now();
                var newHash = getHash(
                        url,
                        lastPolled
                );
                @Nullable PackState state;
                if (newHash != null) {
                    state = new PackState(newHash, pollTime);
                } else {
                    state = null;
                }
                future.complete(state);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }, "BSPReloadThread").start();
        return future;
    }

    static byte[] sha1FromStream(InputStream stream) throws IOException {
        try (var digestStream = new DigestInputStream(stream, MessageDigest.getInstance("SHA-1"))) {
            digestStream.readAllBytes();
            return digestStream.getMessageDigest().digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not find a SHA-1 algorithm even though it is a required JVM feature", e);
        }
    }

    record PackState(@Unmodifiable byte[] hash, Instant polledAt) { }
}
