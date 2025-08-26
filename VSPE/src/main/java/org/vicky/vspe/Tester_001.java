package org.vicky.vspe;

import de.pauleff.api.ICompoundTag;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vicky.platform.*;
import org.vicky.platform.events.PlatformEventFactory;
import org.vicky.platform.utils.ResourceLocation;
import org.vicky.platform.utils.Vec3;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.platform.world.PlatformBlockStateFactory;
import org.vicky.vspe.paper.VSPEBukkitStructureManager;
import org.vicky.vspe.platform.PlatformBiomeFactory;
import org.vicky.vspe.platform.PlatformBlockDataRegistry;
import org.vicky.vspe.platform.PlatformStructureManager;
import org.vicky.vspe.platform.VSPEPlatformPlugin;
import org.vicky.vspe.platform.features.CharmsAndTrinkets.PlatformTrinketManager;
import org.vicky.vspe.platform.features.advancement.PlatformAdvancementManager;
import org.vicky.vspe.platform.systems.dimension.DimensionDescriptor;
import org.vicky.vspe.platform.systems.dimension.PlatformDimensionManager;
import org.vicky.vspe.platform.systems.dimension.globalDimensions.StructureResolvers;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.*;
import org.vicky.vspe.platform.systems.platformquestingintegration.QuestProductionFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class Tester_001 {
    static {
        temporaryInit();
    }

    public static void main(String[] args) {
        var rnd = new SeededRandomSource(2089380289328302L);
        var ctx = new StructurePlacementContext(rnd, org.vicky.platform.utils.Rotation.NONE, org.vicky.platform.utils.Mirror.NONE);
        var placer = new BlockPlacer<String>() {
            @Override
            public int getHighestBlockAt(int i, int i1) {
                return 0;
            }

            @Override
            public void placeBlock(@NotNull Vec3 vec3, @Nullable PlatformBlockState<String> platformBlockState, @NotNull ICompoundTag iCompoundTag) {
                System.out.println("  pos: " + vec3.getX() + "," + vec3.getY() + "," + vec3.getZ() + ", data:" + platformBlockState.getNative());
            }

            @Override
            public void placeBlock(@NotNull Vec3 vec3, @Nullable PlatformBlockState<String> platformBlockState) {
                System.out.println("  pos: [" + vec3.getX() + "," + vec3.getY() + "," + vec3.getZ() + "], data:" + platformBlockState.getNative());
            }

            @Override
            public void placeBlock(int i, int i1, int i2, @Nullable PlatformBlockState<String> platformBlockState, @NotNull ICompoundTag iCompoundTag) {
                System.out.println("  pos: [" + i + "," + i1 + "," + i2 + "], data:" + platformBlockState.getNative());
            }

            @Override
            public void placeBlock(int i, int i1, int i2, @Nullable PlatformBlockState<String> platformBlockState) {
                System.out.println("  pos: [" + i + "," + i1 + "," + i2 + "], data:" + platformBlockState.getNative());
            }
        };

        TESTER.structures.values().forEach(it -> {
            System.out.println("Structure: " + it.getFirst().getClass().getSimpleName());
            it.getFirst().placeInChunk(
                    placer,
                    0, 0,
                    it.getFirst().resolve(new Vec3(0, 0, 0), ctx),
                    ctx
            );
        });
    }

    private static void temporaryInit() {
        PlatformPlugin.set(new PlatformPlugin() {
            @Override
            public PlatformLogger getPlatformLogger() {
                return new PlatformLogger() {
                    @Override
                    public void info(String s) {
                        System.out.println(s);
                    }

                    @Override
                    public void warn(String s) {
                        System.out.println("[WRN]" + s);
                    }

                    @Override
                    public void error(String s) {
                        System.out.println("[ERR]" + s);
                    }

                    @Override
                    public void debug(String s) {
                        System.out.println("[DBG]" + s);
                    }

                    @Override
                    public void error(String s, Throwable throwable) {
                        System.out.println("[ERR]" + s);
                        throwable.printStackTrace();
                    }
                };
            }

            @Override
            public PlatformScheduler getPlatformScheduler() {
                return null;
            }

            @Override
            public PlatformRankService getRankService() {
                return null;
            }

            @Override
            public PlatformParticleProvider getParticleProvider() {
                return null;
            }

            @Override
            public PlatformChatFormatter getChatFormatter() {
                return null;
            }

            @Override
            public PlatformConfig getPlatformConfig() {
                return null;
            }

            @Override
            public PlatformBossBarFactory getPlatformBossBarFactory() {
                return null;
            }

            @Override
            public PlatformBlockStateFactory getPlatformBlockStateFactory() {
                return s -> SimpleBlockState.Companion.from(s, it -> it);
            }

            @Override
            public PlatformItemFactory getPlatformItemFactory() {
                return null;
            }

            @Override
            public PlatformEntityFactory getPlatformEntityFactory() {
                return null;
            }

            @Override
            public PlatformEventFactory getEventFactory() {
                return null;
            }

            @Override
            public PlatformLocationAdapter<?> getPlatformLocationAdapter() {
                return null;
            }

            @Override
            public File getPlatformDataFolder() {
                return null;
            }

            @Override
            public Optional<PlatformPlayer> getPlatformPlayer(UUID uuid) {
                return Optional.empty();
            }
        });
        VSPEPlatformPlugin.set(
                new VSPEPlatformPlugin() {
                    @Override
                    public void registerDimensionDescriptor(DimensionDescriptor descriptor) {

                    }

                    @Override
                    public PlatformScheduler getPlatformScheduler() {
                        return null;
                    }

                    @Override
                    public PlatformStructureManager<?> getPlatformStructureManager() {
                        return new VSPEBukkitStructureManager();
                    }

                    @Override
                    public PlatformBlockDataRegistry<?> getPlatformBlockDataRegistry() {
                        return null;
                    }

                    @Override
                    public PlatformConfig getPlatformConfig() {
                        return null;
                    }

                    @Override
                    public boolean platformIsNative() {
                        return true;
                    }

                    @Override
                    public File getPlatformDataFolder() {
                        return null;
                    }

                    @Override
                    public PlatformLogger getPlatformLogger() {
                        return PlatformPlugin.logger();
                    }

                    @Override
                    public PlatformDimensionManager<?, ?> getDimensionManager() {
                        return null;
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
                        return null;
                    }

                    @Override
                    public <B extends PlatformBiome> PlatformBiomeFactory<B> getPlatformBiomeFactory() {
                        return null;
                    }
                }
        );
    }

    static class TESTER implements PlatformStructureManager<String> {

        private static final Map<ResourceLocation, Pair<PlatformStructure<String>, StructureRule>> structures
                = new HashMap<>();

        static {
            structures.putAll(initStructures());
        }

        private static Map<ResourceLocation, Pair<PlatformStructure<String>, StructureRule>> initStructures() {
            Map<ResourceLocation, Pair<PlatformStructure<String>, StructureRule>> result = new HashMap<>();
            new StructureResolvers<String>().structures.forEach(it -> {
                result.put(it.getSecond().getResource(), it);
            });
            return result;
        }

        @Override
        public @NotNull Map<ResourceLocation, Pair<PlatformStructure<String>, StructureRule>> getStructures() {
            return structures;
        }

        @Override
        public @Nullable NbtStructure<String> getNBTStructure(ResourceLocation resourceLocation) {
            return null;
        }

        @Override
        public @Nullable PlatformStructure<String> getStructure(ResourceLocation id) {
            var struct = structures.get(id).getFirst();
            return struct != null ? struct : new NBTBasedStructure<>(id);
        }

        @Override
        public void addStructure(@NotNull ResourceLocation id, @NotNull PlatformStructure<String> structure, @NotNull StructureRule rule) {
            structures.put(id, new Pair<>(structure, rule));
        }
    }

}
