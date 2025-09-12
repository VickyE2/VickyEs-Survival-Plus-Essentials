package org.vicky.vspe_forge.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.vicky.vspe_forge.VspeForge;
import org.vicky.vspe_forge.dimension.ForgeWorldGenProvider;

import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = VspeForge.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ForgeDataGenerator {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // generator.addProvider(event.includeServer(), new ModRecipeProvider(packOutput));
        // generator.addProvider(event.includeServer(), ModLootTableProvider.create(packOutput));
        // generator.addProvider(event.includeClient(), new ModBlockStateProvider(packOutput, existingFileHelper));
        // generator.addProvider(event.includeClient(), new ModItemModelProvider(packOutput, existingFileHelper));
        // ModBlockTagGenerator blockTagGenerator = generator.addProvider(event.includeServer(),
        //         new ModBlockTagGenerator(packOutput, lookupProvider, existingFileHelper));
        // generator.addProvider(event.includeServer(), new ModItemTagGenerator(packOutput, lookupProvider, blockTagGenerator.contentsGetter(), existingFileHelper));
        // generator.addProvider(event.includeServer(), new ModGlobalLootModifiersProvider(packOutput));
        // generator.addProvider(event.includeServer(), new ModPoiTypeTagsProvider(packOutput, lookupProvider, existingFileHelper));

        generator.addProvider(event.includeServer(), new ForgeWorldGenProvider(packOutput, lookupProvider));
    }
}
