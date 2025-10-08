package org.vicky.vspe_forge.dimension;

import de.pauleff.api.ICompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vicky.platform.utils.Mirror;
import org.vicky.platform.utils.Rotation;
import org.vicky.platform.utils.Vec3;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.vspe.platform.VSPEPlatformPlugin;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.BlockPlacer;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.PlatformStructure;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.StructurePlacementContext;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.StructureRule;
import org.vicky.vspe_forge.VspeForge;

import static org.vicky.vspe_forge.forgeplatform.AwsomeForgeHacks.fromForge;

public class ForgePlatformStructurePiece extends StructurePiece {
    private final PlatformStructure<BlockState> nativeStructure;

    @Override
    public void postProcess(
            @NotNull WorldGenLevel level,
            @NotNull StructureManager templates,
            @NotNull ChunkGenerator generator,
            RandomSource random,
            @NotNull BoundingBox box,
            @NotNull ChunkPos chunkPos,
            @NotNull BlockPos structureCenter
    ) {
        final boolean LOG_STRUCTURE_TIMING = true; // toggle this on/off

        long totalStart = System.nanoTime();

        if (LOG_STRUCTURE_TIMING) {
            VspeForge.LOGGER.info("[STRUCT] ---------------------------------------------");
            VspeForge.LOGGER.info("[STRUCT] Starting placement at chunk {} ({})", chunkPos, structureCenter);
        }

        // 1. Build placement context
        long stepStart = System.nanoTime();
        StructurePlacementContext placementCtx = new StructurePlacementContext(
                fromForge(random),
                switch (random.nextInt(1, 4)) {
                    case 1 -> Rotation.CLOCKWISE_90;
                    case 2 -> Rotation.CLOCKWISE_180;
                    case 3 -> Rotation.COUNTERCLOCKWISE_90;
                    default -> Rotation.NONE;
                },
                switch (random.nextInt(1, 3)) {
                    case 1 -> Mirror.FRONT_BACK;
                    case 2 -> Mirror.LEFT_RIGHT;
                    default -> Mirror.NONE;
                }
        );
        if (LOG_STRUCTURE_TIMING)
            VspeForge.LOGGER.info("[STRUCT] Built placement context in {} ms", elapsedMs(stepStart));

        // 2. Compute structure origin
        stepStart = System.nanoTime();
        Vec3 origin = new Vec3(structureCenter.getX(), structureCenter.getY(), structureCenter.getZ());
        if (LOG_STRUCTURE_TIMING)
            VspeForge.LOGGER.info("[STRUCT] Computed origin {} in {} ms", origin, elapsedMs(stepStart));

        // 3. Resolve the structure
        stepStart = System.nanoTime();
        var resolved = nativeStructure.resolve(origin, placementCtx);
        if (LOG_STRUCTURE_TIMING)
            VspeForge.LOGGER.info("[STRUCT] Resolved structure in {} ms", elapsedMs(stepStart));

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

        // 5. Place blocks belonging to this chunk only
        stepStart = System.nanoTime();
        nativeStructure.place(
                placer,
                origin,
                placementCtx
        );
        if (LOG_STRUCTURE_TIMING)
            VspeForge.LOGGER.info("[STRUCT] Placed chunk blocks in {} ms", elapsedMs(stepStart));

        // Total
        if (LOG_STRUCTURE_TIMING)
            VspeForge.LOGGER.info("[STRUCT] Finished placement for {} in {} ms", chunkPos, elapsedMs(totalStart));
    }    public static final StructurePieceType TYPE = ForgePlatformStructurePiece::new;
    private final StructureRule rule;
    public ForgePlatformStructurePiece(@NotNull BlockPos pos, @NotNull PlatformStructure<BlockState> nativeStructure, @NotNull StructureRule rule) {
        super(TYPE, 0, BoundingBox.fromCorners(
                        pos.offset(-4, 0, -4),
                        pos.offset(nativeStructure.getSize().getX(), nativeStructure.getSize().getY(), nativeStructure.getSize().getZ())
                )
        );
        this.setOrientation(null);
        this.nativeStructure = nativeStructure;
        this.rule = rule;
    }

    public ForgePlatformStructurePiece(StructurePieceSerializationContext structurePieceSerializationContext,
                                       CompoundTag tag) {
        super(TYPE, tag);
        var pair =
                VSPEPlatformPlugin.structureManager().getStructures().values().stream()
                        .filter(it ->
                                it.getSecond().getResource().asString().equals(tag.getString("StructureId")))
                        .findFirst().get();

        this.nativeStructure = (PlatformStructure<BlockState>) pair.getFirst();
        this.rule = pair.getSecond();
    }

    // Utility helper
    private static String elapsedMs(long startTime) {
        return String.format("%.3f", (System.nanoTime() - startTime) / 1_000_000.0);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putString("StructureId", rule.getResource().toString());
    }


}
