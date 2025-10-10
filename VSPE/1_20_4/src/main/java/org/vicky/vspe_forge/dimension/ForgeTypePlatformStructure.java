package org.vicky.vspe_forge.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import org.jetbrains.annotations.NotNull;
import org.vicky.platform.utils.Mirror;
import org.vicky.platform.utils.Rotation;
import org.vicky.platform.utils.Vec3;
import org.vicky.vspe.platform.VSPEPlatformPlugin;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.PlatformStructure;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.StructurePlacementContext;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.StructureRule;
import org.vicky.vspe_forge.VspeForge;
import org.vicky.vspe_forge.registers.Dimensions;

import java.util.Optional;

import static org.vicky.vspe_forge.dimension.CodecCORE.BIOME_HOLDER_SET_CODEC;
import static org.vicky.vspe_forge.forgeplatform.AwsomeForgeHacks.fromForge;

@SuppressWarnings("unchecked")
public class ForgeTypePlatformStructure extends Structure {
    public static final Codec<ForgeTypePlatformStructure> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("resource_location").forGetter(src -> VspeForge.MODID + ":" + src.rule.getResource().getPath()),
            Codec.STRING.fieldOf("previous_namespace").forGetter(src -> src.rule.getResource().getNamespace()),
            settingsCodec(instance),
            BIOME_HOLDER_SET_CODEC
                    .fieldOf("resolved_biomes")
                    .forGetter(s -> s.resolvedBiomes)
    ).apply(instance, (loc, prev, settings, biomes) -> {
        var pair =
                VSPEPlatformPlugin.structureManager().getStructures().values().stream().filter(it -> it.getSecond().getResource().asString().equals(prev + ":" + loc.split(":", 2)[1])).findFirst().get();
        return new ForgeTypePlatformStructure((PlatformStructure<BlockState>) pair.getFirst(), pair.getSecond(), settings, biomes);
    }));

    private final PlatformStructure<BlockState> nativeStructure;
    private final StructureRule rule;
    private final transient HolderSet<Biome> resolvedBiomes;

    public ForgeTypePlatformStructure(PlatformStructure<BlockState> nativeStructure, StructureRule rule, StructureSettings settings, HolderSet<Biome> biomes) {
        super(settings);
        this.nativeStructure = nativeStructure;
        this.rule = rule;
        this.resolvedBiomes = biomes;
    }

    public static TerrainAdjustment toTerrain(StructureRule rule) {
        return switch (rule.getTags()) {
            case RUINS, NETHER, HOUSE -> TerrainAdjustment.BEARD_THIN;
            case DUNGEON, VILLAGE -> TerrainAdjustment.BEARD_BOX;
            case OCEAN, SKY, TREELIKE, EMPTY -> TerrainAdjustment.NONE;
            case FROZEN, ANCIENT -> TerrainAdjustment.BURY;
        };
    }

    public static GenerationStep.Decoration toStep(StructureRule rule) {
        return switch (rule.getTags()) {
            case FROZEN, TREELIKE, SKY, HOUSE, VILLAGE -> GenerationStep.Decoration.SURFACE_STRUCTURES;
            case DUNGEON, NETHER, EMPTY -> GenerationStep.Decoration.UNDERGROUND_STRUCTURES;
            case OCEAN -> GenerationStep.Decoration.LAKES;
            case ANCIENT, RUINS -> GenerationStep.Decoration.STRONGHOLDS;
        };
    }

    @Override
    public @NotNull StructureType<?> type() {
        return Dimensions.PLATFORM_STRUCTURE.get();
    }

    private static @NotNull StructurePlacementContext getStructurePlacementContext(@NotNull GenerationContext context) {
        var random = context.random();
        return new StructurePlacementContext(
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
    }

    @Override
    public @NotNull BoundingBox adjustBoundingBox(@NotNull BoundingBox p_226570_) {
        return super.adjustBoundingBox(p_226570_);
    }

    @Override
    protected @NotNull Optional<GenerationStub> findGenerationPoint(@NotNull GenerationContext context) {
        if (context.random().nextDouble() > rule.getFrequency()) return Optional.empty();
        ChunkPos chunkPos = context.chunkPos();

        // Biome match
        BlockPos pos = chunkPos.getMiddleBlockPosition(0);
        Holder<Biome> biome = context.biomeSource().getNoiseBiome(
                QuartPos.fromBlock(pos.getX()),
                QuartPos.fromBlock(rule.getFixedY() == -1 ? 64 : rule.getFixedY()),
                QuartPos.fromBlock(pos.getZ()),
                context.randomState().sampler()
        );

        boolean biomeMatches = resolvedBiomes.stream()
                .anyMatch(b -> b.unwrapKey().isPresent() && biome.unwrapKey().isPresent() &&
                        b.unwrapKey().get().equals(biome.unwrapKey().get()));
        if (!biomeMatches) return Optional.empty();

        int y;
        switch (rule.getVerticalPlacement()) {
            case SKY -> y = context.chunkGenerator().getFirstFreeHeight(pos.getX(), pos.getZ(),
                    Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState()) + rule.getFixedY();
            case SURFACE -> y = context.chunkGenerator().getFirstFreeHeight(pos.getX(), pos.getZ(),
                    Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
            case UNDERGROUND -> y = context.chunkGenerator().getFirstOccupiedHeight(pos.getX(), pos.getZ(),
                    Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState()) - rule.getFixedY();
            default -> y = 64;
        }

        BlockPos genPos = new BlockPos(pos.getX(), y, pos.getZ());

        // Return final generation stub
        return Optional.of(new GenerationStub(genPos, builder -> {
            StructurePlacementContext placementCtx = getStructurePlacementContext(context);
            var resolved = nativeStructure.resolve(Vec3.of(pos.getX(), y, pos.getZ()), placementCtx);
            for (var chunkCoords : resolved.getPlacementsByChunk().entrySet()) {
                BlockPos piecePos = new BlockPos(chunkCoords.getKey().getCx() * 16, y, chunkCoords.getKey().getCz() * 16);
                builder.addPiece(new ForgePlatformStructurePiece(piecePos, chunkCoords.getValue()));
            }
        }));
    }
}
