package org.vicky.vspe;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.vicky.platform.PlatformConfig;
import org.vicky.platform.PlatformLogger;
import org.vicky.platform.PlatformPlugin;
import org.vicky.platform.PlatformScheduler;
import org.vicky.vspe.forge.forgeplatform.*;
import org.vicky.vspe.forge.registers.Blocks;
import org.vicky.vspe.forge.registers.Items;
import org.vicky.vspe.forge.registers.Tabs;
import org.vicky.vspe.platform.PlatformBiomeFactory;
import org.vicky.vspe.platform.PlatformBlockDataRegistry;
import org.vicky.vspe.platform.PlatformStructureManager;
import org.vicky.vspe.platform.VSPEPlatformPlugin;
import org.vicky.vspe.platform.features.CharmsAndTrinkets.PlatformTrinketManager;
import org.vicky.vspe.platform.features.advancement.PlatformAdvancementManager;
import org.vicky.vspe.platform.systems.dimension.DimensionDescriptor;
import org.vicky.vspe.platform.systems.dimension.PlatformDimensionManager;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.PlatformBiome;
import org.vicky.vspe.platform.systems.platformquestingintegration.QuestProductionFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(VspeForge.MODID)
public class VspeForge implements VSPEPlatformPlugin {
    public static final String MODID = "vspe";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static MinecraftServer server;
    public static RegistryAccess registryAccess;

    public VspeForge() {
        VSPEPlatformPlugin.set(this);
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
        Blocks.BLOCKS.register(modEventBus);
        Blocks.BLOCK_ITEMS.register(modEventBus);
        Items.ITEMS.register(modEventBus);
        Tabs.CREATIVE_MODE_TABS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
        try {
            ForgeStructureManager.createInstance(Minecraft.getInstance().getResourceManager());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerAboutToStartEvent event) {
        // Do something when the server starts
        server = event.getServer();
        registryAccess = event.getServer().registryAccess();
        LOGGER.info("HELLO from server starting");
        ForgeDimensionManager.prepareGenerators();
        ForgeDimensionManager.getInstance().loadDimensionsFromDescriptors();
    }

    @Override
    public void registerDimensionDescriptor(DimensionDescriptor dimensionDescriptor) {

    }

    @Override
    public PlatformScheduler getPlatformScheduler() {
        return PlatformPlugin.scheduler();
    }

    @Override
    public PlatformStructureManager<?> getPlatformStructureManager() {
        return ForgeStructureManager.getInstance();
    }

    @Override
    public PlatformBlockDataRegistry<?> getPlatformBlockDataRegistry() {
        return ForgeBlockDataRegistry.INSTANCE;
    }

    @Override
    public PlatformConfig getPlatformConfig() {
        return VSPEForgePlatformConfig.getInstance();
    }

    @Override
    public boolean platformIsNative() {
        return false;
    }

    @Override
    public File getPlatformDataFolder() {
        Path dataFolderPath = FMLPaths.CONFIGDIR.get().resolve(MODID);
        try {
            Files.createDirectories(dataFolderPath); // safe: only creates if missing
        } catch (IOException e) {
            throw new RuntimeException("Failed to create data folder for " + MODID, e);
        }
        return dataFolderPath.toFile();
    }

    @Override
    public PlatformLogger getPlatformLogger() {
        return new VSPEForgeLogger();
    }

    @Override
    public PlatformDimensionManager<?, ?> getDimensionManager() {
        return ForgeDimensionManager.getInstance();
    }

    @Override
    public PlatformTrinketManager<?> getPlatformTrinketManager() {
        return null;
    }

    @Override
    public QuestProductionFactory getQuestProductionFactory() {
        return null;
    }

    @Override
    public PlatformAdvancementManager<?> getPlatformAdvancementManager() {
        return ForgeAdvancementManager.getInstance();
    }

    @Override
    public <B extends PlatformBiome> PlatformBiomeFactory<B> getPlatformBiomeFactory() {
        return (PlatformBiomeFactory<B>) new ForgeBiomeFactory();
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
