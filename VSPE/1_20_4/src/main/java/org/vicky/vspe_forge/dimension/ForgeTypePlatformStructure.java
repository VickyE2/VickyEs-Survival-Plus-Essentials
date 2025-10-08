package org.vicky.vspe_forge.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import org.jetbrains.annotations.NotNull;
import org.vicky.platform.utils.ResourceLocation;
import org.vicky.vspe.platform.VSPEPlatformPlugin;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.PlatformStructure;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.StructureRule;
import org.vicky.vspe_forge.VspeForge;
import org.vicky.vspe_forge.registers.Dimensions;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ForgeTypePlatformStructure extends Structure {
    public static final Codec<ForgeTypePlatformStructure> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("resource_location").forGetter(src -> src.rule.getResource().asString())
    ).apply(instance, (loc) -> {
        var pair =
                VSPEPlatformPlugin.structureManager().getStructures().values().stream().filter(it -> it.getSecond().getResource().asString().equals(loc)).findFirst().get();
        return new ForgeTypePlatformStructure((PlatformStructure<BlockState>) pair.getFirst(), pair.getSecond());
    }));

    private final PlatformStructure<BlockState> nativeStructure;
    private final StructureRule rule;
    private final List<ResourceLocation> biomeKeys;
    private transient HolderSet<Biome> resolvedBiomes;

    public ForgeTypePlatformStructure(PlatformStructure<BlockState> nativeStructure, StructureRule rule) {
        super(new StructureSettings(
                HolderSet.direct(),
                Map.of(),
                toStep(rule),
                toTerrain(rule)
        ));
        this.nativeStructure = nativeStructure;
        this.rule = rule;
        this.biomeKeys = rule.getBiomes().stream().toList();
    }

    private static TerrainAdjustment toTerrain(StructureRule rule) {
        return switch (rule.getTags()) {
            case RUINS, NETHER, HOUSE -> TerrainAdjustment.BEARD_THIN;
            case DUNGEON, VILLAGE -> TerrainAdjustment.BEARD_BOX;
            case OCEAN, SKY, TREELIKE, EMPTY -> TerrainAdjustment.NONE;
            case FROZEN, ANCIENT -> TerrainAdjustment.BURY;
        };
    }

    private static GenerationStep.Decoration toStep(StructureRule rule) {
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

    @Override
    protected @NotNull Optional<GenerationStub> findGenerationPoint(@NotNull GenerationContext context) {
        if (resolvedBiomes == null) {
            HolderGetter<Biome> biomeGetter = context.registryAccess().lookupOrThrow(Registries.BIOME);
            resolvedBiomes = HolderSet.direct(biomeKeys.stream()
                    .map(it -> ((ForgeBiome) VSPEPlatformPlugin.biomeFactory().getFor(it).orElseThrow()).getResourceKey())
                    .map(biomeGetter::getOrThrow)
                    .toList());
        }

        VspeForge.LOGGER.info("Resolved structure biomes: {}", resolvedBiomes.stream()
                .map(b -> b.unwrapKey().map(ResourceKey::location).orElse(null))
                .toList());

        // if (context.random().nextDouble() > rule.getFrequency()) return Optional.empty();
        ChunkPos chunkPos = context.chunkPos();

        // Biome match
        BlockPos pos = chunkPos.getMiddleBlockPosition(0);
        Holder<Biome> biome = context.biomeSource().getNoiseBiome(
                QuartPos.fromBlock(pos.getX()),
                QuartPos.fromBlock(rule.getFixedY() == -1 ? 64 : rule.getFixedY()),
                QuartPos.fromBlock(pos.getZ()),
                context.randomState().sampler()
        );
        VspeForge.LOGGER.info("Biome at pos {} is {}", pos, biome.unwrapKey().map(ResourceKey::location).orElse(null));

        boolean biomeMatches = resolvedBiomes.stream()
                .anyMatch(b -> b.unwrapKey().isPresent() && biome.unwrapKey().isPresent() &&
                        b.unwrapKey().get().equals(biome.unwrapKey().get()));
        // if (!biomeMatches) return Optional.empty();


        // Height placement
        VspeForge.LOGGER.info("Starting to choose Y Placement");
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
        VspeForge.LOGGER.info("We finna chose {}", y);

        BlockPos genPos = new BlockPos(pos.getX(), y, pos.getZ());

        // Return final generation stub
        return Optional.of(new GenerationStub(genPos, builder -> {
            long start = System.currentTimeMillis();
            VspeForge.LOGGER.info("Structure piece placement started {} ms", start);
            builder.addPiece(new ForgePlatformStructurePiece(genPos, nativeStructure, rule));
            long end = System.currentTimeMillis();
            VspeForge.LOGGER.info("Structure piece placement took {} ms", (end - start));
        }));
    }
}
