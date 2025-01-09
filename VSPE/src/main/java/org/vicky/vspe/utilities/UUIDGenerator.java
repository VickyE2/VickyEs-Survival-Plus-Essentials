package org.vicky.vspe.utilities;

import java.util.UUID;

public class UUIDGenerator {
    public static UUID generateUUIDFromString(String input) {
        // Use a namespace UUID (you can use a fixed one or generate a custom one)
        UUID namespace = UUID.nameUUIDFromBytes("namespace".getBytes());
        return UUID.nameUUIDFromBytes((namespace + input).getBytes());
    }
}
