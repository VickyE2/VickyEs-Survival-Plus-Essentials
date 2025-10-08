package org.vicky.vspe_forge.dimension;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import org.vicky.vspe_forge.VspeForge;
import org.vicky.vspe_forge.forgeplatform.ForgeBiomeFactory;
import org.vicky.vspe_forge.forgeplatform.ForgeStructureManager;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ForgeWorldGenProvider extends DatapackBuiltinEntriesProvider {
    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            // .add(Registries.CONFIGURED_FEATURE, ModConfiguredFeatures::bootstrap)
            // .add(Registries.PLACED_FEATURE, ModPlacedFeatures::bootstrap)
            // .add(ForgeRegistries.Keys.BIOME_MODIFIERS, ModBiomeModifiers::bootstrap)
            .add(Registries.BIOME, ForgeBiomeFactory::boostrap)
            .add(Registries.STRUCTURE_SET, ForgeStructureManager::boostrap)
            .add(Registries.STRUCTURE, ForgeStructureManager::registerPlatformStructure)
            // .add(Registries.BIOME_SOURCE, CodecCORE::bootstrapBiomes)
            // .add(Registries.CHUNK_GENERATOR, CodecCORE::bootstrapGenerators)
            .add(Registries.DIMENSION_TYPE, WorldManager::applyDimensionTypes)
            .add(Registries.LEVEL_STEM, WorldManager::applyLevelStems);

    public ForgeWorldGenProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(VspeForge.MODID, "crymorra"));
    }
}
