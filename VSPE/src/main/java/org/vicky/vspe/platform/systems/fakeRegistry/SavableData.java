package org.vicky.vspe.platform.systems.fakeRegistry;

import com.google.gson.JsonObject;

public interface SavableData {
    String getKey();

    JsonObject serialize();
}
