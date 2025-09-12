package org.vicky.vspe_forge.objects;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.ticks.ScheduledTick;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class SmartVineBlock extends Block {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 2); // 0..2 -> user 1..3

    // shapes for each stage (example - tweak to taste)
    private static final VoxelShape[] SHAPES = new VoxelShape[] {
            Block.box(2, 0, 2, 14, 6, 14),  // small (stage 0)
            Block.box(1, 0, 1, 15, 10, 15), // medium (stage 1)
            Block.box(0, 0, 0, 16, 16, 16)  // large (stage 2)
    };

    public SmartVineBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        int s = state.getValue(STAGE);
        return SHAPES[Math.max(0, Math.min(s, SHAPES.length - 1))];
    }

    // Called when placed or neighbor changes -> schedule an update tick to recompute stage
    @Override
    public void neighborChanged(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Block neighborBlock, @NotNull BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 1);
        }
    }

    // scheduled tick - recompute and update state if needed
    @Override
    public void tick(BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource rand) {
        int newStage = computeStage(level, pos);
        int cur = state.getValue(STAGE);
        if (newStage != cur) {
            // set new stage, keep other properties if any
            level.setBlock(pos, state.setValue(STAGE, newStage), 3); // 3 = notify clients + cause block update
        }
    }

    /**
     * Compute stage (0..2) based on the block above and below.
     * Default logic:
     *  - If above/below are the same SmartVineBlock, use their stage values.
     *  - Otherwise, map common blocks: air -> 0 (small), leaves/plants -> 1, solid ground -> 2 (large).
     *  - Final stage = clamp( round((aboveStage + belowStage) / 2.0) ), but you can change to median or other logic.
     */
    protected int computeStage(LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockPos below = pos.below();

        int aboveStage = mapBlockToStage(level, above);
        int belowStage = mapBlockToStage(level, below);

        // Example rule: use the average and round to nearest
        double avg = (aboveStage + belowStage) / 2.0;
        int stage = (int)Math.round(avg);

        // ensure it's between 0 and 2
        if (stage < 0) stage = 0;
        if (stage > 2) stage = 2;
        return stage;
    }

    /**
     * Map a neighboring block to a stage value (0..2).
     * If the neighbor is the same vine and has a stage property, return that stage.
     * Otherwise, map types:
     *  - air -> 0
     *  - plants/leaves -> 1
     *  - solid blocks -> 2
     *
     * Tweak this to match your design (e.g., check for specific items/blocks).
     */
    protected int mapBlockToStage(LevelReader level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        Block b = s.getBlock();

        if (b instanceof SmartVineBlock) {
            return s.getValue(STAGE); // neighbor stage
        }

        if (s.isAir()) return 0;

        // simple heuristic: tend toward larger if the neighbor can support the vine
        if (s.is(BlockTags.LEAVES) || s.is(Blocks.VINE) || s.is(BlockTags.FLOWERS)) return 1;

        // treat generally solid blocks as stage 2
        if (s.isSolidRender(level, pos)) return 2;

        // fallback
        return 1;
    }
}
