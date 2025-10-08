package org.vicky.vspe_forge.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;
import org.jetbrains.annotations.NotNull;
import org.vicky.vspe_forge.dimension.ForgeWorldGenProvider;
import org.vicky.vspe_forge.dimension.UnImpressedBiomeSource;
import org.vicky.vspe_forge.dimension.UnImpressedChunkGenerator;
import org.vicky.vspe_forge.forgeplatform.ForgeAdvancementManager;
import org.vicky.vspe_forge.forgeplatform.ForgeDimensionManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.vicky.vspe_forge.VspeForge.MODID;
import static org.vicky.vspe_forge.VspeForge.registryAccess;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ForgeDataGenerator {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        registryAccess = event.getLookupProvider().join();
        CompletableFuture<HolderLookup.Provider> chained = getProviderCompletableFuture(event);

        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        // Add the datagen provider which will write the jsons to the output.
        generator.addProvider(event.includeServer(), new ForgeWorldGenProvider(packOutput, chained));
    }

    @SubscribeEvent
    public static void onNewRegistry(RegisterEvent event) {
        // VspeForge.LOGGER.info("Registering codecs...hehe");
        event.register(Registries.CHUNK_GENERATOR, helper -> {
            helper.register("unimpressed",
                    UnImpressedChunkGenerator.CODEC);
        });
        event.register(Registries.BIOME_SOURCE, helper -> {
            helper.register("unimpressed",
                    UnImpressedBiomeSource.CODEC);
        });
    }

    private static @NotNull CompletableFuture<HolderLookup.Provider> getProviderCompletableFuture(GatherDataEvent event) {
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        CompletableFuture<HolderLookup.Provider> chained =
                lookupProvider.thenApplyAsync(provider -> {
                    // Run your delayed setup logic here
                    try {
                        TimeUnit.SECONDS.sleep(1); // wait 1 second
                    } catch (InterruptedException ignored) {
                    }

                    ForgeAdvancementManager.getInstance();
                    ForgeDimensionManager.prepareGenerators();
                    ForgeDimensionManager.getInstance().loadDimensionsFromDescriptors();

                    return provider; // pass it forward
                });
        return chained;
    }
}
