package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions;

import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.world.WritableWorld;
import com.dfsek.terra.api.world.chunk.generation.ProtoChunk;
import de.pauleff.api.ICompoundTag;
import org.jetbrains.annotations.NotNull;
import org.vicky.platform.PlatformPlayer;
import org.vicky.platform.utils.ResourceLocation;
import org.vicky.platform.utils.Vec3;
import org.vicky.platform.world.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TerraPlatformWorld implements PlatformWorld<String, WritableWorld> {

    private final WritableWorld world;
    private final Map<Long, PlatformBlockState<String>> placed = new ConcurrentHashMap<>();

    public TerraPlatformWorld(WritableWorld world) {
        this.world = world;
    }

    @Override
    public String getName() {
        return world.getPack().getNamespace();
    }

    @Override
    public WritableWorld getNative() {
        return world;
    }

    @Override
    public int getHighestBlockYAt(double x, double z) {
        return world.getMaxHeight();
    }

    @Override
    public int getMaxWorldHeight() {
        return world.getMaxHeight();
    }

    @Override
    public List<PlatformPlayer> getPlayers() {
        return List.of();
    }

    @Override
    public PlatformBlock<String> getBlockAt(double x, double y, double z) {
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        int zi = (int) Math.floor(z);

        // 1) If we have a placed value in our local cache, return that
        long key = key(xi, yi, zi);
        PlatformBlockState<String> cached = placed.get(key);
        if (cached != null) {
            return new TerraBlock((TerraBlockState) cached, new PlatformLocation(this, x, y, z));
        }

        // 2) Otherwise try to query Terra world block state (if API available)
        try {
            // Many Terra WritableWorld implementations have something like getBlockState(int,int,int)
            BlockState terraState = world.getBlockState(xi, yi, zi);

            if (terraState != null) {
                TerraBlockState tbs = new TerraBlockState(terraState);
                return new TerraBlock(tbs, new PlatformLocation(this, x, y, z));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 3) fallback: return a default "air" block state
        PlatformBlockState<String> air = createPlatformBlockState("minecraft:air", "");
        return new TerraBlock(new TerraBlockState(air), new PlatformLocation(this, x, y, z));
    }

    @Override
    public PlatformBlock<String> getBlockAt(Vec3 vec3) {
        // fixed bug: use vec3.x, vec3.y, vec3.z and create PlatformLocation with same coordinates
        return getBlockAt(vec3.x, vec3.y, vec3.z);
    }

    @Override
    public PlatformBlockState<String> getAirBlockState() {
        return createPlatformBlockState("minecraft:air", "");
    }

    @Override
    public PlatformBlockState<String> getWaterBlockState() {
        return createPlatformBlockState("minecraft:water", "");
    }

    @Override
    public PlatformBlockState<String> createPlatformBlockState(String id, String properties) {
        // simple PlatformBlockState implementation that stores id and properties map
        return new PlatformBlockState<>() {
            private final String nid = id;
            private final Map<String, String> props = TerraPlatformWorld.parseProperties(properties);

            @Override
            public String getId() {
                return nid;
            }

            @Override
            public PlatformMaterial getMaterial() {
                return new PlatformMaterial() {

                    @Override
                    public boolean isSolid() {
                        return true;
                    }

                    @Override
                    public boolean isAir() {
                        return false;
                    }

                    @Override
                    public ResourceLocation getResourceLocation() {
                        return ResourceLocation.from(nid);
                    }
                };
            }

            @Override
            public String getNative() {
                // For this generic implementation, just return the id as the "native"
                return nid;
            }

            @Override
            public Map<String, String> getProperties() {
                return props;
            }

            @Override
            public <P> P getProperty(String name) {
                Object v = props.get(name);
                return (P) v;
            }
        };
    }

    @Override
    public void loadChunkIfNeeded(int i, int i1) {
        world.getGenerator().generateChunkData(
                new ProtoChunk() {
                    @Override
                    public int getMaxHeight() {
                        return getMaxWorldHeight();
                    }

                    @Override
                    public void setBlock(int i, int i1, int i2, @NotNull BlockState blockState) {
                        setPlatformBlockState(new Vec3(i, i1, i2), new TerraBlockState(blockState));
                    }

                    @Override
                    public @NotNull BlockState getBlock(int i, int i1, int i2) {
                        return ((TerraBlockState) getBlockAt(i, i1, i2)).getUnderlyingBlockStateObject();
                    }

                    @Override
                    public Object getHandle() {
                        return null;
                    }
                },
                world.buffer(i, 0, i1),
                world.getBiomeProvider(),
                i, i1
        );
    }

    @Override
    public boolean isChunkLoaded(int i, int i1) {
        return true;
    }

    @Override
    public void setPlatformBlockState(Vec3 vec3, PlatformBlockState platformBlockState, ICompoundTag iCompoundTag) {
        setPlatformBlockState(vec3, platformBlockState); // ignoring NBT tag for now
    }

    @Override
    public void setPlatformBlockState(Vec3 position, PlatformBlockState state) {
        int x = (int) Math.floor(position.x);
        int y = (int) Math.floor(position.y);
        int z = (int) Math.floor(position.z);
        long key = key(x, y, z);

        // 1) If the incoming state is a TerraBlockState wrapping Terra's BlockState, attempt to call Terra API:
        if (state instanceof TerraBlockState tbs) {
            BlockState terraNative = tbs.getUnderlyingBlockStateObject(); // we add this helper below

            try {
                world.setBlockState(x, y, z, terraNative);
            } catch (Exception ex) {
                placed.put(key, state);
                return;
            }

            placed.put(key, state);
            return;
        }

        // 2) For generic platform states (not TerraBlockState), store in local cache and attempt to resolve to Terra BlockType later
        placed.put(key, state);

    }

    private static Map<String, String> parseProperties(String properties) {
        // properties string format e.g. "facing=north,age=2"
        Map<String, String> map = new ConcurrentHashMap<>();
        if (properties == null || properties.isEmpty()) return map;
        String[] entries = properties.split(",");
        for (String e : entries) {
            String[] kv = e.split("=", 2);
            if (kv.length == 2) map.put(kv[0].trim(), kv[1].trim());
        }
        return map;
    }

    private static long key(int x, int y, int z) {
        // pack three ints into a long (simple, collisions unlikely in our domain)
        long lx = x & 0xFFFF_FFFFL;
        long ly = y & 0xFFFFL;
        long lz = z & 0xFFFFL;
        return (lx << 32) ^ (ly << 16) ^ lz;
    }
}