package org.vicky.vspe.addon.util;

import com.dfsek.terra.api.Platform;
import com.dfsek.terra.api.registry.key.Keyed;
import com.dfsek.terra.api.registry.key.RegistryKey;
import com.dfsek.terra.api.structure.Structure;
import com.dfsek.terra.api.util.Rotation;
import com.dfsek.terra.api.util.vector.Vector3Int;
import com.dfsek.terra.api.world.WritableWorld;

import java.util.Random;

public abstract class BaseStructure implements Structure, Keyed<BaseStructure> {
    protected Platform platform;
    private final String id;

    public BaseStructure(String id) {
        this.id = id;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    @Override
    public String getID() {
        return id;
    }
    public String getId() {
        return id;
    }

    protected abstract void generateSteps(Vector3Int vector3Int, WritableWorld writableWorld, Random random, Rotation rotation);

    @Override
    public boolean generate(Vector3Int vector3Int, WritableWorld writableWorld, Random random, Rotation rotation) {
        generateSteps(vector3Int, writableWorld, random, rotation);

        return true;
    }

    @Override
    public RegistryKey getRegistryKey() {
        return RegistryKey.of("vspe", id);
    }
}
