package org.vicky.vspe_forge.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import org.vicky.vspe.platform.VSPEPlatformPlugin;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.PlatformStructure;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.StructureRule;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ForgeTypePlatformStructure extends Structure {
    public static final Codec<ForgeTypePlatformStructure> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("resource_location").forGetter(src -> src.rule.getResource().asString())
    ).apply(instance, (loc) -> {
        var pair =
                VSPEPlatformPlugin.structureManager().getStructures().values().stream().filter(it -> it.getSecond().getResource().asString().equals(loc)).findFirst().get();
        return new ForgeTypePlatformStructure(pair.getFirst(), pair.getSecond());
    }));

    private final PlatformStructure<BlockState> nativeStructure;
    private final StructureRule rule;

    protected ForgeTypePlatformStructure(PlatformStructure<BlockState> nativeStructure, StructureRule rule) {
        super(new StructureSettings(
                HolderSet.direct(
                        rule.getBiomes().stream()
                                .map(it -> VSPEPlatformPlugin.biomeFactory().getFor(it).orElse(null))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList())
                ),
                Map.of(),
                rule.
        ));
        this.nativeStructure = nativeStructure;
        this.rule = rule;
    }

    private static TerrainAdjustment toTerrain(StructureRule rule) {
        return switch (rule.getTerrainType()) {
            case BEARD_THIN -> TerrainAdjustment.BEARD_THIN;
            case BEARD_BOX -> TerrainAdjustment.BEARD_BOX;
            case BURY -> TerrainAdjustment.BURY;
            default -> TerrainAdjustment.NONE;
        };
    }

    @Override
    public StructureType<?> type() {
        return null;
    }

    private static GenerationStep.Decoration toStep(StructureRule rule) {
        return switch (rule.getTags().) {
            case UNDERGROUND -> GenerationStep.Decoration.UNDERGROUND_STRUCTURES;
            case AIR -> GenerationStep.Decoration.TOP_LAYER_MODIFICATION;
            default -> GenerationStep.Decoration.SURFACE_STRUCTURES;
        };
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {

        // Apply your extra settings
        if (context.chunkPos().getMinBlockY() < settings.minY()) {
            return Optional.empty(); // too low
        }

        // Maybe use spawnDensity for randomness
        if (context.random().nextDouble() > settings.spawnDensity()) {
            return Optional.empty();
        }

        return onTopOfChunkCenter(context, Heightmap.Types.WORLD_SURFACE_WG, builder -> {
            builder.addPiece(new MyStructurePiece(context.chunkPos().getMiddleBlockPosition(0)));
        });
    }
}
