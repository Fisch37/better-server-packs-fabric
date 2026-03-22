package de.fisch37.betterserverpacksfabric.pack_downloaders;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@NullMarked
public class HttpDownloader implements PackDownloader {
    @Override @Contract(value = "_,null -> !null")
    public byte @Nullable [] getHash(URL url, @Nullable Instant lastPolled) throws IOException {
        var conn = url.openConnection();
        if (!(conn instanceof HttpURLConnection http)) {
            throw new IllegalArgumentException("URL is not http");
        }
        if (lastPolled != null) {
            http.addRequestProperty(
                    "If-Modified-Since",
                    lastPolled.atZone(ZoneOffset.UTC)
                            .format(DateTimeFormatter.RFC_1123_DATE_TIME)
            );
        }
        http.connect();
        var responseCode = http.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            return PackDownloader.sha1FromStream(http.getInputStream());
        } else if (lastPolled != null && responseCode == 304) {
            // 304 Not Modified
            return null;
        } else {
            throw new InvalidResponseException(
                    responseCode,
                    "Invalid response code " + responseCode + " to pack request"
            );
        }
    }
}
