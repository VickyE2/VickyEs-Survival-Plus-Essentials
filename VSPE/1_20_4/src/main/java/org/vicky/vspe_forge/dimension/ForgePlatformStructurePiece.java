package org.vicky.vspe_forge.dimension;

import de.pauleff.api.ICompoundTag;
import de.pauleff.api.NBTFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vicky.forge.forgeplatform.useables.ForgePlatformBlockStateAdapter;
import org.vicky.platform.utils.Vec3;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.BlockPlacement;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.BlockPlacer;
import org.vicky.vspe_forge.VspeForge;
import org.vicky.vspe_forge.registers.Dimensions;

import java.util.ArrayList;
import java.util.List;

public class ForgePlatformStructurePiece extends StructurePiece {
    public static final StructurePieceType TYPE =
            Dimensions.FORGE_PLATFORM_PIECE.get();
    private final BlockPos origin;
    private final @NotNull List<BlockPlacement<BlockState>> blockPlacements;

    public ForgePlatformStructurePiece(@NotNull BlockPos pos, @NotNull List<BlockPlacement<BlockState>> blockPlacements) {
        super(TYPE, 0, computeBoundingBox(pos, blockPlacements));
        this.setOrientation(null);
        this.origin = pos;
        this.blockPlacements = blockPlacements;
    }

    public ForgePlatformStructurePiece(StructurePieceSerializationContext structurePieceSerializationContext,
                                       CompoundTag tag) {
        super(TYPE, tag);
        this.origin = new BlockPos(
                tag.getInt("OriginX"),
                tag.getInt("OriginY"),
                tag.getInt("OriginZ")
        );

        ListTag placementList = tag.getList("placements", Tag.TAG_COMPOUND);
        List<BlockPlacement<BlockState>> blockPlacements = new ArrayList<>();
        for (int i = 0; i < placementList.size(); i++) {
            CompoundTag single = placementList.getCompound(i);
            int[] posArray = single.getIntArray("pos");
            if (posArray.length < 3) continue;

            Vec3 pos = new Vec3(posArray[0], posArray[1], posArray[2]);

            // You’ll need to resolve the state string -> BlockState
            String stateStr = single.getString("state");
            BlockState state =
                    ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation(stateStr)).defaultBlockState();

            // optional NBT
            ICompoundTag blockNbt = null;
            if (single.contains("nbt")) {
                try {
                    blockNbt = NBTFactory.parseCompoundFromSNBT(single.getString("nbt"));
                } catch (Exception e) {
                    // ignore or log
                }
            }

            blockPlacements.add(new BlockPlacement<>(pos.getIntX(), pos.getIntY(), pos.getIntZ(),
                    new ForgePlatformBlockStateAdapter(state), blockNbt));
        }

        this.blockPlacements = blockPlacements;
    }

    // Utility helper
    private static String elapsedMs(long startTime) {
        return String.format("%.3f", (System.nanoTime() - startTime) / 1_000_000.0);
    }

    private static BoundingBox computeBoundingBox(BlockPos origin, List<BlockPlacement<BlockState>> placements) {
        if (placements.isEmpty()) {
            return new BoundingBox(origin);
        }

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPlacement<BlockState> p : placements) {
            Vec3 pos = p.getVecPosition();
            minX = Math.min(minX, pos.getIntX());
            minY = Math.min(minY, pos.getIntY());
            minZ = Math.min(minZ, pos.getIntZ());
            maxX = Math.max(maxX, pos.getIntX());
            maxY = Math.max(maxY, pos.getIntY());
            maxZ = Math.max(maxZ, pos.getIntZ());
        }

        return BoundingBox.fromCorners(new BlockPos(minX - 1, minY - 1, minZ - 1), new BlockPos(maxX + 1, maxY + 1, maxZ + 1));
    }

    @Override
    protected void addAdditionalSaveData(@NotNull StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("OriginX", origin.getX());
        tag.putInt("OriginY", origin.getY());
        tag.putInt("OriginZ", origin.getZ());

        ListTag placementList = new ListTag();
        for (BlockPlacement<BlockState> p : blockPlacements) {
            if (p.getState() != null) {
                CompoundTag single = new CompoundTag();
                Vec3 pos = p.getVecPosition().round();
                single.putIntArray("pos", new int[]{pos.getIntX(), pos.getIntY(), pos.getIntZ()});
                single.putString("state", p.getState().getNative().getBlock().toString().replace("Block{", "").replace("}", ""));
                if (p.getNbt() != null) {
                    single.putString("nbt", NBTFactory.toSNBT(p.getNbt()));
                }
                placementList.add(single);
            }
        }
        tag.put("placements", placementList);
    }

    @Override
    public void postProcess(
            @NotNull WorldGenLevel level,
            @NotNull StructureManager templates,
            @NotNull ChunkGenerator generator,
            @NotNull RandomSource random,
            @NotNull BoundingBox box,
            @NotNull ChunkPos chunkPos,
            @NotNull BlockPos structureCenter
    ) {
        final boolean LOG_STRUCTURE_TIMING = false; // toggle this on/off

        long totalStart = System.nanoTime();

        if (LOG_STRUCTURE_TIMING) {
            VspeForge.LOGGER.info("[STRUCT] ---------------------------------------------");
            VspeForge.LOGGER.info("[STRUCT] Starting placement at chunk {} ({})", chunkPos, structureCenter);
        }

        // 1. Build placement context
        long stepStart = System.nanoTime();

        final int[] times = {0};

        // 4. Build BlockPlacer
        stepStart = System.nanoTime();
        BlockPlacer<BlockState> placer = new BlockPlacer<>() {
            @Override
            public void placeBlock(int i, int i1, int i2, @Nullable PlatformBlockState<BlockState> state) {
                if (state != null) {
                    if (LOG_STRUCTURE_TIMING && times[0] < 10) {
                        VspeForge.LOGGER.info("It wasn't null and is this: {}", state.getNative());
                        times[0]++;
                    }
                    level.setBlock(
                            new BlockPos(i, i1, i2),
                            state.getNative(),
                            Block.UPDATE_ALL
                    );
                }
                if (LOG_STRUCTURE_TIMING && times[0] < 10) {
                    VspeForge.LOGGER.info("It was null");
                    times[0]++;
                }
            }

            @Override
            public void placeBlock(int i, int i1, int i2, @Nullable PlatformBlockState<BlockState> state, @NotNull ICompoundTag tag) {
                placeBlock(i, i1, i2, state);
            }

            @Override
            public void placeBlock(@NotNull Vec3 vec3, @Nullable PlatformBlockState<BlockState> state) {
                placeBlock(vec3.getIntX(), vec3.getIntY(), vec3.getIntZ(), state);
            }

            @Override
            public void placeBlock(@NotNull Vec3 vec3, @Nullable PlatformBlockState<BlockState> state, @NotNull ICompoundTag tag) {
                placeBlock(vec3.getIntX(), vec3.getIntY(), vec3.getIntZ(), state);
            }

            @Override
            public int getHighestBlockAt(int x, int z) {
                return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            }
        };

        if (LOG_STRUCTURE_TIMING)
            VspeForge.LOGGER.info("[STRUCT] Created BlockPlacer in {} ms", elapsedMs(stepStart));

        stepStart = System.nanoTime();
        for (var placement : blockPlacements) {
            placer.placeBlock(placement.getVecPosition(), placement.getState(), placement.getNbt());
        }
        if (LOG_STRUCTURE_TIMING)
            VspeForge.LOGGER.info("[STRUCT] Placed chunk blocks in {} ms", elapsedMs(stepStart));

        // Total
        if (LOG_STRUCTURE_TIMING)
            VspeForge.LOGGER.info("[STRUCT] Finished placement for {} in {} ms", chunkPos, elapsedMs(totalStart));
    }
}
