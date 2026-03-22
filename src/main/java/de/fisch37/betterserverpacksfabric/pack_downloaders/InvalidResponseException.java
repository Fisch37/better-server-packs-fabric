package de.fisch37.betterserverpacksfabric.pack_downloaders;

import java.io.IOException;

public class InvalidResponseException extends IOException {
    public final int code;

    public InvalidResponseException(int code, String message) {
        super(message);
        this.code = code;
    }
}
