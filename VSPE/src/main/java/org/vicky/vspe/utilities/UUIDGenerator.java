package org.vicky.vspe.utilities;

import java.util.UUID;

public class UUIDGenerator {
   public static UUID generateUUIDFromString(String input) {
      UUID namespace = UUID.nameUUIDFromBytes("namespace".getBytes());
      return UUID.nameUUIDFromBytes((namespace + input).getBytes());
   }
}
