package org.vicky.vspe_forge.objects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SmartVineBlock extends Block {
    // NOTE: the IntegerProperty may be created at runtime in this class's constructor.
    // That works as long as you register one Block instance per desired maxStage.
    public static final int STATIC_MAX_STAGE = 7;
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, STATIC_MAX_STAGE);
    private final int maxStage;
    private final VoxelShape[] shapes;

    // Growth tuning
    private final double baseGrowthChance = 0.06; // base per-randomTick chance (tweak)
    private final double coldMultiplier;
    private final double hotMultiplier;
    private final boolean useTemperatureMultiplier;

    // If non-null, only grow when the block directly above equals this block
    // (or is the specified Block). If null -> always allowed to grow (subject to above not-air).
    private final @Nullable Block requiredBlockOnTop;
    private final boolean isClimbable;

    public SmartVineBlock(BlockBehaviour.Properties props, int maxStage,
                          @Nullable Block requiredBlockOnTop,
                          boolean useTemperatureMultiplier,
                          double coldMultiplier,
                          double hotMultiplier, boolean isClimbable) {
        super(props);
        this.maxStage = Math.min(maxStage, STATIC_MAX_STAGE);
        this.shapes = generateShapes(this.maxStage);
        // temperature multipliers
        this.useTemperatureMultiplier = useTemperatureMultiplier;
        this.coldMultiplier = coldMultiplier;
        this.hotMultiplier = hotMultiplier;
        this.requiredBlockOnTop = requiredBlockOnTop;
        this.isClimbable = isClimbable;
        // initialize default state
        this.registerDefaultState(this.stateDefinition.any().setValue(STAGE, 0));
    }

    // convenience constructor with defaults
    public SmartVineBlock(BlockBehaviour.Properties props, int maxStage, @Nullable Block requiredBlockOnTop, boolean useTemperatureMultiplier) {
        this(props, maxStage, requiredBlockOnTop, useTemperatureMultiplier, 0.75, 0.15, true);
    }

    public SmartVineBlock(BlockBehaviour.Properties props) {
        this(props, 3, null, true, 0.75, 0.15, true);
    }

    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, net.minecraft.world.entity.LivingEntity entity) {
        return isClimbable;
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        // Empty shape = walk through
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }

    @Override
    public @NotNull VoxelShape getVisualShape(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        // No obstruction to visuals
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }

    @Override
    public boolean isCollisionShapeFullBlock(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        return false;
    }

    private VoxelShape[] generateShapes(int maxStage) {
        VoxelShape[] arr = new VoxelShape[maxStage + 1];
        for (int stage = 0; stage <= maxStage; stage++) {
            double growth = (stage / (double) Math.max(1, maxStage));
            double inset = (1.0 - growth) * 8.0; // smaller -> fuller
            double height = 4.0 + (growth * 12.0); // 4 -> 16
            arr[stage] = Block.box(inset, 0, inset, 16 - inset, height, 16 - inset);
        }
        return arr;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext ctx) {
        int s = Math.max(0, Math.min(state.getValue(STAGE), shapes.length - 1));
        return shapes[s];
    }

    // schedule an initial tick when placed or neighbor change
    @Override
    public void neighborChanged(@NotNull BlockState state, Level level, @NotNull BlockPos pos,
                                @NotNull Block neighborBlock, @NotNull BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) return;
        if (level instanceof ServerLevel serverLevel) {
            tryGrowOrBreak(state, serverLevel, pos, serverLevel.random);
        } else {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void onPlace(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean isMoving) {
        if (level.isClientSide) return;
        if (level instanceof ServerLevel serverLevel) {
            tryGrowOrBreak(state, serverLevel, pos, serverLevel.random);
        } else {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource rand) {
        // server-only scheduled tick - use it to attempt growth / break rules
        tryGrowOrBreak(state, level, pos, rand);
        level.scheduleTick(pos, this, 20 + rand.nextInt(40)); // 1-3s for growth attempts
    }

    // Also support random growth via randomTick (if world random ticks are enabled)
    @Override
    public void randomTick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource rand) {
        tick(state, level, pos, rand);
    }

    // Growth / break logic centralised
    private void tryGrowOrBreak(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        if (level.isClientSide) return;

        int ruleStage = computeStageFromColumnLength(level, pos);

        if (ruleStage == -1) {
            // break if rule says so
            level.removeBlock(pos, true);
            level.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, defaultBlockState()),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    10, 0.1, 0.1, 0.1, 0.1);
            return;
        }

        // Ensure we follow structural rules
        if (ruleStage != state.getValue(STAGE)) {
            level.setBlock(pos, state.setValue(STAGE, Math.min(maxStage - 1, ruleStage)), 3);
        }

        // Optional: still perform growth logic (so vines spread downward naturally)
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);

        if (aboveState.isAir()) {
            level.removeBlock(pos, true);
            return;
        }

        // Temperature multiplier etc. (keep your logic here)
        double tempMul = 1.0;
        if (useTemperatureMultiplier) {
            Biome biome = level.getBiome(pos).value();
            float temp = biome.getBaseTemperature();
            if (temp <= 0.3f) tempMul = coldMultiplier;
            else if (temp >= 0.75f) tempMul = hotMultiplier;
        }

        double effectiveChance = baseGrowthChance * tempMul;
        if (rand.nextDouble() >= effectiveChance) return;

        int curStage = state.getValue(STAGE);

        if (belowState.isAir()) {
            if (curStage < maxStage) {
                level.setBlock(pos, state.setValue(STAGE, curStage + 1), 3);
            } else {
                BlockState newState = defaultBlockState().setValue(STAGE, 0);
                if (level.setBlock(below, newState, 3)) {
                    level.scheduleTick(below, this, 1);
                }
            }
        } else if (belowState.getBlock() == this) {
            int belowStage = belowState.getValue(STAGE);
            if (belowStage < maxStage) {
                level.setBlock(below, belowState.setValue(STAGE, belowStage + 1), 3);
            }
        }
    }

    private int computeStageFromColumnLength(Level level, BlockPos pos) {
        // break check
        BlockPos above = pos.above();
        if (level.getBlockState(above).isAir()) return -1;

        // count continuous vine blocks downward (including self)
        int length = 0;
        BlockPos cursor = pos;
        while (length <= maxStage - 1 && level.getBlockState(cursor).getBlock() instanceof SmartVineBlock) {
            length++;
            cursor = cursor.below();
        }

        // Now map length -> stage 0..maxStage (simple linear mapping)
        // length 1 -> stage 0, length maxStage+1 -> maxStage (adjust to taste)
        int stage = (int) Math.round(((length - 1) / (double) Math.max(1, maxStage - 1)) * maxStage - 1);
        stage = Math.max(0, Math.min(maxStage - 1, stage));
        return stage;
    }

    private int computeStageFromNeighbors(Level level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockPos below = pos.below();

        BlockState aboveState = level.getBlockState(above);
        BlockState belowState = level.getBlockState(below);

        boolean aboveIsVine = aboveState.getBlock() instanceof SmartVineBlock;
        boolean belowIsVine = belowState.getBlock() instanceof SmartVineBlock;

        // 1. No block on top -> break
        if (aboveState.isAir()) return -1;

        // 2. Above not vine, below not vine → stage 0
        if (!aboveIsVine && !belowIsVine) return scaleRuleStageToMax(0, maxStage - 1);

        // 3. Above not vine, below is vine, two below not vine → stage 1
        BlockPos twoBelow = below.below();
        BlockState twoBelowState = level.getBlockState(twoBelow);
        boolean twoBelowIsVine = twoBelowState.getBlock() instanceof SmartVineBlock;
        if (!aboveIsVine && belowIsVine && !twoBelowIsVine) return scaleRuleStageToMax(1, maxStage);

        // 4. Above is vine, below not vine → stage 0
        if (aboveIsVine && !belowIsVine) return scaleRuleStageToMax(0, maxStage - 1);

        // 5. Above is vine, below is vine, deeper below is vine -> stage 2 (long column)
        if (aboveIsVine && belowIsVine && twoBelowIsVine) return scaleRuleStageToMax(2, maxStage - 1);

        // 5b. Above is vine, below is vine (shorter column) → stage 1
        if (aboveIsVine && belowIsVine) return scaleRuleStageToMax(1, maxStage - 1);

        // 6. Above not vine, below vine with stage 1 → stage 2
        if (!aboveIsVine && belowIsVine) {
            int belowStage = belowState.hasProperty(STAGE) ? belowState.getValue(STAGE) : 0;
            if (belowStage == 1) return scaleRuleStageToMax(2, maxStage - 1);
        }

        // default
        return scaleRuleStageToMax(0, maxStage - 1);
    }

    private int scaleRuleStageToMax(int ruleStage /* 0..2 */, int maxStage) {
        // Map 0..2 -> 0..maxStage proportionally
        if (ruleStage <= 0) return 0;
        if (ruleStage >= 2) return maxStage;
        // ruleStage == 1 -> middle
        return Math.max(1, (int) Math.round((ruleStage / 2.0) * maxStage));
    }
}
