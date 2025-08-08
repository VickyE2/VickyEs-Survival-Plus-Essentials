package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions;

import com.dfsek.terra.api.block.state.BlockState;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.platform.world.PlatformMaterial;

import java.util.HashMap;
import java.util.Map;

public class TerraBlockState implements PlatformBlockState<String> {

    private final BlockState terraState;    // may be null for "pure id" states
    private final String id;
    private final Map<String, String> properties;

    public TerraBlockState(BlockState terraStateObj) {
        // If you pass Terra's BlockState instance, store it (cast)
        BlockState bs = null;
        if (terraStateObj != null) {
            bs = terraStateObj;
        }
        this.terraState = bs;
        if (bs != null) {
            this.id = bs.getAsString();
        } else {
            this.id = "unknown";
        }
        this.properties = new HashMap<>();
    }

    // constructor for simple id-based states
    public TerraBlockState(PlatformBlockState<String> simple) {
        this.terraState = null;
        this.id = simple.getId();
        this.properties = new HashMap<>(simple.getProperties());
    }

    @Override
    public String getId() {
        return terraState.getBlockType().getDefaultState().getAsString();
    }

    @Override
    public PlatformMaterial getMaterial() {
        return new TerraMaterial(terraState.getBlockType().getDefaultState().getAsString().split(":", 1)[1]);
    }

    @Override
    public String getNative() {
        return terraState.getAsString();
    }

    @Override
    public Map<String, String> getProperties() {
        return new HashMap<>();
    }

    @Override
    public <P> P getProperty(String name) {
        return null;
    }

    /**
     * Helper for Terra-specific integration: returns underlying BlockState object when available.
     * Caller must check isTerraBacked() before using.
     */
    public BlockState getUnderlyingBlockStateObject() {
        return terraState;
    }

    /**
     * Convenience: return optional boolean if underlying Terra state can indicate solidity.
     */
    public Boolean isSolid() {
        return terraState.getBlockType().isSolid();
    }

    public boolean isTerraBacked() {
        return terraState != null;
    }
}
