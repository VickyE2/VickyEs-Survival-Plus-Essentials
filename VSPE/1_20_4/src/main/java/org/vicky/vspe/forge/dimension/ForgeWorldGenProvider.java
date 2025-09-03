package org.vicky.vspe.forge.dimension;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import org.vicky.vspe.VspeForge;
import org.vicky.vspe.forge.forgeplatform.ForgeBiomeFactory;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ForgeWorldGenProvider extends DatapackBuiltinEntriesProvider {
    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.DIMENSION_TYPE, WorldManager::applyDimensionTypes)
            // .add(Registries.CONFIGURED_FEATURE, ModConfiguredFeatures::bootstrap)
            // .add(Registries.PLACED_FEATURE, ModPlacedFeatures::bootstrap)
            // .add(ForgeRegistries.Keys.BIOME_MODIFIERS, ModBiomeModifiers::bootstrap)
            .add(Registries.BIOME, ForgeBiomeFactory::boostrap)
            .add(Registries.LEVEL_STEM, WorldManager::applyLevelStems);

    public ForgeWorldGenProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(VspeForge.MODID));
    }
}
