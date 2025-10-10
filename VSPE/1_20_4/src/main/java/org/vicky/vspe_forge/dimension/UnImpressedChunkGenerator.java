package org.vicky.vspe_forge.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.core.*;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jetbrains.annotations.NotNull;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.vspe.platform.VSPEPlatformPlugin;
import org.vicky.vspe.platform.systems.dimension.DimensionDescriptor;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.ChunkHeightProvider;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.MultiParameterBiomeResolver;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

public class UnImpressedChunkGenerator extends ChunkGenerator {
    public static final Codec<UnImpressedChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UnImpressedBiomeSource.CODEC.fieldOf("biome_source").forGetter(gen -> (UnImpressedBiomeSource) gen.biomeSource),
            Codec.LONG.fieldOf("seed").forGetter(gen -> gen.seed)
    ).apply(instance, UnImpressedChunkGenerator::new));
    private static final Map<String, ChunkHeightProvider> heightProviderCache = new ConcurrentHashMap<>();
    private final long seed;
    private final DimensionDescriptor descriptor;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final java.util.concurrent.ConcurrentMap<ChunkPos, GenerationDebug> DEBUG_MAP = new java.util.concurrent.ConcurrentHashMap<>();

    public UnImpressedChunkGenerator(UnImpressedBiomeSource biomeSource, long seed) {
        super(biomeSource);
        this.seed = seed;
        this.descriptor = biomeSource.descriptor;
    }

    private static GenerationDebug debugFor(ChunkPos pos) {
        return DEBUG_MAP.computeIfAbsent(pos, k -> {
            GenerationDebug g = new GenerationDebug();
            g.startTimeMs = System.currentTimeMillis();
            g.lastUpdateMs = g.startTimeMs;
            g.threadName = Thread.currentThread().getName();
            return g;
        });
    }

    private static void clearDebug(ChunkPos pos) {
        DEBUG_MAP.remove(pos);
    }

    private static void logProgress(String s) {
        // VSPEPlatformPlugin.platformLogger().info("[UnImprGen] " + s);
    }

    private static void setBlockInSectionFast(ChunkAccess chunk, BlockPos pos, BlockState state) {
        int sectionIndex = chunk.getSectionIndex(pos.getY());          // chunk.getSectionIndex(y) -> section array index
        LevelChunkSection section = chunk.getSection(sectionIndex);
        int localX = pos.getX() & 15;
        int localY = pos.getY() & 15;     // y % 16
        int localZ = pos.getZ() & 15;
        section.setBlockState(localX, localY, localZ, state, false); // ``false`` = don't update ref counts / lighting
    }

    @Override
    protected @NotNull Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    private static int fetchReferences(StructureManager manager, ChunkAccess chunk, SectionPos sectionPos, Structure structure) {
        StructureStart existing = manager.getStartForStructure(sectionPos, structure, chunk);
        return existing != null ? existing.getReferences() : 0;
    }

    @Override
    public void applyCarvers(@NotNull WorldGenRegion region, long seed, @NotNull RandomState random, @NotNull BiomeManager biomeManager, @NotNull StructureManager structureManager, @NotNull ChunkAccess chunk, GenerationStep.@NotNull Carving carving) {

    }

    @Override
    public void buildSurface(@NotNull WorldGenRegion region, @NotNull StructureManager structureManager, @NotNull RandomState random, @NotNull ChunkAccess chunk) {

    }

    @Override
    public void spawnOriginalMobs(@NotNull WorldGenRegion region) {

    }

    @Override
    public int getGenDepth() {
        return getSeaLevel() - getMinY();
    }

    // === Biomes phase (like vanilla createBiomes) ===
    @Override
    public @NotNull CompletableFuture<ChunkAccess> createBiomes(@NotNull Executor executor, @NotNull RandomState randomState,
                                                                @NotNull Blender blender, @NotNull StructureManager structureManager,
                                                                @NotNull ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("init_biomes", () -> {
            logProgress("UnImpressedChunkGenerator.createBiomes for chunk " + chunk.getPos());
            GenerationDebug g = debugFor(chunk.getPos());
            g.setStage(GenStage.BIOMES_INIT);
            doCreateBiomes(blender, randomState, chunk);
            return chunk;
        }), Util.backgroundExecutor());
    }

    // === Fill phase ===
    @Override
    public @NotNull CompletableFuture<ChunkAccess> fillFromNoise(@NotNull Executor executor, @NotNull Blender blender,
                                                                 @NotNull RandomState noiseConfig, @NotNull StructureManager structureAccessor,
                                                                 ChunkAccess chunk) {
        logProgress("UnImpressedChunkGenerator.createNoise for chunk " + chunk.getPos());
        GenerationDebug g = debugFor(chunk.getPos());
        g.setStage(GenStage.FILLING_INIT);
        // Acquire sections we will touch (mirror vanilla section acquire)
        int minSectionIndex = chunk.getSectionIndex(getMinY());
        int maxSectionIndex = chunk.getSectionIndex(getSeaLevel() - 1);

        // Clamp to chunk section bounds
        int sectionCount = chunk.getSectionsCount();
        minSectionIndex = Math.max(0, Math.min(sectionCount - 1, minSectionIndex));
        maxSectionIndex = Math.max(0, Math.min(sectionCount - 1, maxSectionIndex));

        Set<LevelChunkSection> acquired = new HashSet<>();
        for (int i = maxSectionIndex; i >= minSectionIndex; --i) {
            LevelChunkSection s = chunk.getSection(i);
            s.acquire();
            acquired.add(s);
        }

        // spawn async job (use background executor like vanilla)
        return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("wgen_unimpressed_fill", () -> {
            try {
                return doFill(blender, structureAccessor, noiseConfig, chunk);
            } catch (Throwable e) {
                GenerationDebug gd = debugFor(chunk.getPos());
                gd.setException(e);
                throw new RuntimeException(e);
            }
        }), Util.backgroundExecutor()).whenCompleteAsync((c, t) -> {
            for (LevelChunkSection sec : acquired) sec.release();
        }, executor);
    }

    private void doCreateBiomes(Blender blender, RandomState randomState,
                                ChunkAccess chunk) {
        // Use biome resolver from the RandomState if available (vanilla does this)
        BiomeResolver resolver = blender.getBiomeResolver(this.biomeSource);
        chunk.fillBiomesFromNoise(resolver, randomState.sampler());
        GenerationDebug g = debugFor(chunk.getPos());
        g.setBiomesFilled();
    }


    @Override
    public int getSeaLevel() {
        if (((UnImpressedBiomeSource) biomeSource).getBiomeProvider() instanceof MultiParameterBiomeResolver<ForgeBiome> resolver)
            return (int) (resolver.getSeaLevel() * 319);
        return descriptor.oceanLevel();
    }

    @Override
    public int getMinY() {
        // fixed to -64 as you had previously
        return descriptor.minimumY();
    }

    @Override
    public int getBaseHeight(int x, int z, @NotNull Heightmap.Types heightmap,
                             @NotNull LevelHeightAccessor world, @NotNull RandomState noiseConfig) {

        // determine chunk & index
        int chunkX = Math.floorDiv(x, 16);
        int chunkZ = Math.floorDiv(z, 16);
        int localX = Math.floorMod(x, 16);
        int localZ = Math.floorMod(z, 16);
        int idx = localX + localZ * 16;

        // Find the biome from our biome source
        ForgeBiome biome = ((UnImpressedBiomeSource) biomeSource).getBiomeProvider().resolveBiome(x, 0, z, seed);
        ChunkHeightProvider provider = getOrCreateProviderForBiome(biome);
        int[] heights = computeSmoothedHeights(provider, chunkX, chunkZ);

        int minY = getMinY();
        int maxY = world.getHeight();
        int topY = minY;

        if (idx >= 0 && idx < heights.length) {
            int candidate = heights[idx];
            if (candidate >= minY && candidate < maxY) {
                BlockState forgeData = biome.getDistributionPalette().getFor(localX, candidate, localZ, topY, getSeaLevel()).getNative();
                if (heightmap.isOpaque().test(forgeData)) {
                    topY = candidate;
                }
            }
        }

        return topY;
    }

    @Override
    public @NotNull NoiseColumn getBaseColumn(int x, int z, @NotNull LevelHeightAccessor world, @NotNull RandomState noiseConfig) {
        int minY = getMinY();
        int total = world.getHeight();
        BlockState[] array = new BlockState[total];
        Arrays.fill(array, Blocks.AIR.defaultBlockState());

        // compute topY from provider/palette
        ForgeBiome biome = ((UnImpressedBiomeSource) biomeSource).getBiomeProvider().resolveBiome(x, 0, z, seed);
        int chunkX = Math.floorDiv(x, 16);
        int chunkZ = Math.floorDiv(z, 16);
        ChunkHeightProvider provider = getOrCreateProviderForBiome(biome);
        int[] heights = provider.getChunkHeights(chunkX, chunkZ);
        int localX = Math.floorMod(x, 16);
        int localZ = Math.floorMod(z, 16);
        int idx = localX + localZ * 16;

        if (heights != null && idx >= 0 && idx < heights.length) {
            int topY = heights[idx];
            for (int y = minY; y <= topY && y < minY + total; y++) {
                PlatformBlockState<BlockState> p = biome.getDistributionPalette().getFor(localX, y, localZ, topY, getSeaLevel());
                BlockState bd = p.getNative();
                array[y - minY] = bd;
            }
        }

        return new NoiseColumn(minY, array);
    }

    // -------------------------
    // Replace your heavy logic inside helpers:
    // -------------------------
    private PlatformBlockState<BlockState> getBlockForHeight(int localX, int y, int localZ, int topY, ForgeBiome[] padded, int paddedW, int idx) {
        // Look up biome from padded grid. Keep this lightweight, avoid registry lookups here.
        ForgeBiome biome = padded[(localX + 3) + (localZ + 3) * paddedW]; // example if radius=3; adapt accordingly
        if (biome == null) return null;
        return biome.getDistributionPalette().getFor(localX, y, localZ, topY, getSeaLevel()); // keep this fast
    }

    private ChunkAccess doFill(Blender blender, StructureManager structureAccessor,
                               RandomState noiseConfig, ChunkAccess chunk) {
        // Basic local helpers & precomputation
        GenerationDebug g = debugFor(chunk.getPos());
        g.setStage(GenStage.FILLING);
        ChunkPos pos = chunk.getPos();
        int minY = getMinY();
        int seaLevel = getSeaLevel();
        int startX = pos.getMinBlockX();
        int startZ = pos.getMinBlockZ();

        // Precompute padded biome grid once per chunk
        int radius = 3;
        long t0 = System.nanoTime();
        ForgeBiome[] padded;
        try {
            logProgress("precomputePaddedBiomeGrid START chunk " + pos);
            padded = precomputePaddedBiomeGrid(startX, startZ, seed, radius);
            long t1 = System.nanoTime();
            logProgress(String.format("precomputePaddedBiomeGrid DONE chunk %s time=%.3fms", pos, (t1 - t0) / 1_000_000.0));
        } catch (Throwable ex) {
            logProgress("precomputePaddedBiomeGrid EX chunk " + pos + " -> " + ex);
            throw ex;
        }
        int paddedW = 16 + 2 * radius;

        // Precompute topY per column
        t0 = System.nanoTime();
        int[] topYs;
        try {
            logProgress("computeBiomeAwareBlendedHeightsOptimized START chunk " + pos);
            topYs = computeBiomeAwareBlendedHeightsOptimized(pos.x, pos.z, startX, startZ, padded, radius);
            long t1 = System.nanoTime();
            logProgress(String.format("computeBiomeAwareBlendedHeightsOptimized DONE chunk %s time=%.3fms", pos, (t1 - t0) / 1_000_000.0));
        } catch (Throwable ex) {
            logProgress("computeBiomeAwareBlendedHeightsOptimized EX chunk " + pos + " -> " + ex);
            throw ex;
        }

        // Reuse mutable objects
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        Heightmap hmMotion = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING);
        Heightmap hmMotionNoLeaves = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);
        Heightmap hmOceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG); // or OCEAN_FLOOR

        t0 = System.nanoTime();
        logProgress("UnImpressedChunkGenerator: doFill chunk before locals " + pos);
        // Fill columns
        for (int localZ = 0; localZ < 16; localZ++) {
            for (int localX = 0; localX < 16; localX++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                int idx = localX + localZ * 16;

                int topY = topYs[idx];
                if (topY < minY) topY = minY;
                if (topY >= chunk.getHeight()) topY = chunk.getHeight() - 1;

                // BEDROCK at minY
                mpos.set(worldX, minY, worldZ);
                t0 = System.nanoTime();
                logProgress("place block state pos " + mpos);
                setBlockInSectionFast(chunk, mpos, Blocks.BEDROCK.defaultBlockState());
                long t1 = System.nanoTime();
                logProgress(String.format("block state chunk DONE pos %s time=%.3fms", mpos, (t1 - t0) / 1_000_000.0));

                t0 = System.nanoTime();
                logProgress("hmMotion.update pos " + mpos);
                hmMotion.update(localX, minY, localZ, Blocks.BEDROCK.defaultBlockState());
                t1 = System.nanoTime();
                logProgress(String.format("hmMotion.update DONE pos %s time=%.3fms", mpos, (t1 - t0) / 1_000_000.0));

                t0 = System.nanoTime();
                logProgress("hmMotionNoLeaves.update pos " + mpos);
                hmMotionNoLeaves.update(localX, minY, localZ, Blocks.BEDROCK.defaultBlockState());
                t1 = System.nanoTime();
                logProgress(String.format("hmMotionNoLeaves.update DONE pos %s time=%.3fms", mpos, (t1 - t0) / 1_000_000.0));

                t0 = System.nanoTime();
                logProgress("hmOceanFloor.update pos " + mpos);
                hmOceanFloor.update(localX, minY, localZ, Blocks.BEDROCK.defaultBlockState());
                t1 = System.nanoTime();
                logProgress(String.format("hmOceanFloor.update DONE pos %s time=%.3fms", mpos, (t1 - t0) / 1_000_000.0));

                // Fill body
                logProgress("UnImpressedChunkGenerator: doFill chunk after locals " + pos);
                for (int y = minY + 1; y <= topY; y++) {
                    PlatformBlockState<BlockState> platformBlock = getBlockForHeight(localX, y, localZ, topY, padded, paddedW, idx);
                    if (platformBlock == null) continue;
                    BlockState bs = platformBlock.getNative();
                    mpos.set(worldX, y, worldZ);
                    setBlockInSectionFast(chunk, mpos, bs);

                    // update heightmaps
                    hmMotion.update(localX, y, localZ, bs);
                    hmMotionNoLeaves.update(localX, y, localZ, bs);
                    hmOceanFloor.update(localX, y, localZ, bs);
                }

                // Fill water below sea level if needed (only where air)
                if (topY < seaLevel - 1) {
                    PlatformBlockState<?> water = descriptor.water();
                    BlockState waterState = water.getNative() instanceof BlockState s ? s : null;
                    if (waterState != null) {
                        for (int y = topY + 1; y < seaLevel; y++) {
                            mpos.set(worldX, y, worldZ);
                            if (chunk.getBlockState(mpos).isAir()) {
                                setBlockInSectionFast(chunk, mpos, waterState);
                                hmOceanFloor.update(localX, y, localZ, waterState);
                            }
                        }
                    }
                }
            }
        }

        java.util.function.LongFunction<Long> seedForChunk = (salt) -> {
            long h = seed;
            h ^= (pos.x * 0x9E3779B97F4A7C15L);
            h ^= (pos.z * 0xC6BC279692B5CC83L);
            h = Long.rotateLeft(h, 31);
            h ^= salt;
            return h;
        };

        // Place per-column features (invoke your existing feature placement but avoid heavy allocations)
        g.setStage(GenStage.FEATURES);

        g.setStage(GenStage.DONE);
        return chunk;
    }

    @Override
    public int getSpawnHeight(@NotNull LevelHeightAccessor world) {
        // Return the average "sea level" or something a bit higher
        return Math.max(getSeaLevel() + 5, getMinY() + 64);
    }

    @Override
    public void applyBiomeDecoration(
            @NotNull WorldGenLevel level,
            @NotNull ChunkAccess chunk,
            @NotNull StructureManager structureManager
    ) {
        ChunkPos chunkPos = chunk.getPos();

        BoundingBox bounds = new BoundingBox(
                chunkPos.getMinBlockX(), getMinY(),
                chunkPos.getMinBlockZ(),
                chunkPos.getMaxBlockX(), getSeaLevel() + 50,
                chunkPos.getMaxBlockZ()
        );

        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(level.getSeed()));

        structureManager.startsForStructure(chunkPos, structure -> true).forEach(start -> {
            if (start.isValid()) {
                try {
                    start.placeInChunk(
                            level,              // The level being generated
                            structureManager,   // Structure manager
                            this,               // The chunk generator
                            random,             // RNG
                            bounds,             // Bounding box for placement
                            chunkPos            // Chunk position
                    );
                } catch (Exception e) {
                    System.err.println("⚠ Error placing structure " + start + " in chunk " + chunkPos);
                    e.printStackTrace();
                }
            }
        });
    }

    // Call this from your generator's createStructures(...) override or similar generation entrypoint
    @Override
    public void createStructures(@NotNull RegistryAccess registryAccess,
                                 ChunkGeneratorStructureState chunkStructState,
                                 @NotNull StructureManager structureManager,
                                 ChunkAccess chunk,
                                 @NotNull StructureTemplateManager templateManager) {
        ChunkPos chunkPos = chunk.getPos();
        SectionPos sectionPos = SectionPos.bottomOf(chunk);
        RandomState randomState = chunkStructState.randomState();

        // For each StructureSet that could generate here
        chunkStructState.possibleStructureSets().forEach(structureSetHolder -> {
            StructurePlacement placement = structureSetHolder.value().placement();
            List<StructureSet.StructureSelectionEntry> entries = structureSetHolder.value().structures();

            // Quick bail if any existing start already covers this chunk for any of the set's structures
            for (StructureSet.StructureSelectionEntry sel : entries) {
                StructureStart existing = structureManager.getStartForStructure(sectionPos, sel.structure().value(), chunk);
                if (existing != null && existing.isValid()) return;
            }

            // If chunk is marked as a structure chunk by placement - try generate
            if (placement.isStructureChunk(chunkStructState, chunkPos.x, chunkPos.z)) {
                if (entries.size() == 1) {
                    tryGenerateStructure(entries.getFirst(), structureManager, registryAccess, randomState, templateManager,
                            chunkStructState.getLevelSeed(), chunk, chunkPos, sectionPos);
                } else {
                    // Weighted pick among entries (vanilla behavior)
                    ArrayList<StructureSet.StructureSelectionEntry> arrayList = new ArrayList<>(entries);
                    WorldgenRandom worldgenRandom = new WorldgenRandom(new LegacyRandomSource(0L));
                    worldgenRandom.setLargeFeatureSeed(chunkStructState.getLevelSeed(), chunkPos.x, chunkPos.z);
                    int totalWeight = arrayList.stream().mapToInt(StructureSet.StructureSelectionEntry::weight).sum();

                    while (!arrayList.isEmpty()) {
                        int pick = worldgenRandom.nextInt(totalWeight);
                        int idx = 0;
                        for (; idx < arrayList.size(); idx++) {
                            pick -= arrayList.get(idx).weight();
                            if (pick < 0) break;
                        }
                        var chosen = arrayList.get(idx);
                        if (tryGenerateStructure(chosen, structureManager, registryAccess, randomState, templateManager,
                                chunkStructState.getLevelSeed(), chunk, chunkPos, sectionPos)) {
                            return; // structure generated, don't try others
                        }
                        totalWeight -= chosen.weight();
                        arrayList.remove(idx);
                    }
                }
            }
        });
    }

    private boolean tryGenerateStructure(StructureSet.StructureSelectionEntry sel,
                                         StructureManager structureManager,
                                         RegistryAccess registryAccess,
                                         RandomState randomState,
                                         StructureTemplateManager templateManager,
                                         long levelSeed,
                                         ChunkAccess chunk,
                                         ChunkPos chunkPos,
                                         SectionPos sectionPos) {

        Structure structure = sel.structure().value();

        // fetch existing references (vanilla uses references count to influence generation)
        int references = fetchReferences(structureManager, chunk, sectionPos, structure);

        HolderSet<Biome> biomes = structure.biomes();
        Predicate<Holder<Biome>> biomePredicate = biomes::contains;

        // structure.generate(...) -> returns StructureStart
        StructureStart start = structure.generate(registryAccess, this, this.biomeSource, randomState, templateManager,
                levelSeed, chunkPos, references, chunk, biomePredicate);

        if (start.isValid()) {
            // store the start in the StructureManager for this section (vanilla does exactly this)
            structureManager.setStartForStructure(sectionPos, structure, start, chunk);
            return true;
        } else {
            return false;
        }
    }


    @Override
    public void addDebugScreenInfo(@NotNull List<String> text, @NotNull RandomState noiseConfig, @NotNull BlockPos pos) {
        // Show generator info for the chunk the player is looking at (pos)
        ChunkPos cp = new ChunkPos(Math.floorDiv(pos.getX(), 16), Math.floorDiv(pos.getZ(), 16));
        GenerationDebug gd = DEBUG_MAP.get(cp);
        if (gd != null) {
            text.add("UnImpressedChunkGen: " + gd.shortStatus(cp));
        } else {
            // if none found, optionally list some nearby debug entries
            text.add("UnImpressedChunkGen: no debug for chunk " + cp);
            // list up to 3 recent entries close to player
            DEBUG_MAP.entrySet().stream()
                    .filter(e -> Math.abs(e.getKey().x - cp.x) <= 2 && Math.abs(e.getKey().z - cp.z) <= 2)
                    .limit(3)
                    .forEach(e -> text.add(" nearby: " + e.getValue().shortStatus(e.getKey())));
        }
    }

    /**
     * Return cached provider for this biome or create it lazily.
     * Keying here uses the biome's registry key (string) — replace with a better stable key if you have one.
     */
    private ChunkHeightProvider getOrCreateProviderForBiome(ForgeBiome biome) {
        String key = biome.getIdentifier();
        return heightProviderCache.computeIfAbsent(key, k -> createProviderFromBiome(biome));
    }

    /**
     * Construct a ChunkHeightProvider for the given biome.
     * Implement this to build the provider using the biome's noise layers / parameters.
     */
    private ChunkHeightProvider createProviderFromBiome(ForgeBiome biome) {
        return new ChunkHeightProvider(biome.getHeightSampler());
    }

    /**
     * Smooth a chunk's 16x16 height array by sampling a 3x3 neighborhood of chunks
     * and applying a small weighted kernel. Returns a new int[256].
     * <p>
     * provider.getChunkHeights(chunkX, chunkZ) must return int[256] (localX + localZ*16).
     */
    private int[] computeSmoothedHeights(ChunkHeightProvider provider, int chunkX, int chunkZ) {
        // kernel weights:
        // [1 2 1]
        // [2 4 2]
        // [1 2 1]  -> sum = 16
        final int[] weights = new int[]{
                1, 2, 1,
                2, 4, 2,
                1, 2, 1
        };

        // gather 3x3 neighbor heights (if provider returns null, substitute an all-minY array)
        int[][] neigh = new int[9][];
        int idx = 0;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int cx = chunkX + dx;
                int cz = chunkZ + dz;
                int[] arr = provider.getChunkHeights(cx, cz);
                if (arr == null) {
                    // fallback: fill with minY so missing neighbor won't produce extreme jumps
                    arr = new int[16 * 16];
                    Arrays.fill(arr, getMinY());
                }
                neigh[idx++] = arr;
            }
        }

        int[] out = new int[16 * 16];

        // for each local cell, combine same local coords from neighbors
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int cellIndex = lx + lz * 16;
                int sum = 0;
                for (int k = 0; k < 9; k++) {
                    int w = weights[k];
                    int[] a = neigh[k];
                    // using same local index in neighbor chunk (no sub-chunk offset)
                    sum += a[cellIndex] * w;
                }
                out[cellIndex] = Math.floorDiv(sum, 16); // normalize kernel sum
            }
        }

        return out;
    }

    /**
     * A simple heuristic mapping two biomes -> similarity weight [0..1].
     * Replace with a real tag/category check if you have one.
     */
    private double biomeSimilarityWeight(ForgeBiome a, ForgeBiome b) {
        if (a == null || b == null) return 0.0;

        // identical -> full weight
        if (a.getIdentifier().equals(b.getIdentifier())) return 1.0;

        String ida = a.getIdentifier().toLowerCase(Locale.ROOT);
        String idb = b.getIdentifier().toLowerCase(Locale.ROOT);

        // treat oceans / deep as very dissimilar to land (lower weight).
        boolean aOcean = ida.contains("ocean") || ida.contains("deep_ocean");
        boolean bOcean = idb.contains("ocean") || idb.contains("deep_ocean");

        if (aOcean && bOcean) return 1.0;       // both oceans -> blend well
        if (aOcean ^ bOcean) return 0.15;       // ocean <-> land -> low blend (allows beaches if combined with slope)
        // treat beaches/shores as a bridge
        if (ida.contains("beach") || idb.contains("beach")) return 0.9;

        // fallback: same family (e.g., "forest", "jungle") boost (substring heuristic)
        if (ida.split("/")[0].equals(idb.split("/")[0])) return 0.9;

        // default moderate blend
        return 0.6;
    }


    /**
     * Precompute a padded biome array of size (16 + 2*radius)^2 for world coords:
     * x in [startX - radius .. startX + 15 + radius]
     * z in [startZ - radius .. startZ + 15 + radius]
     * <p>
     * Returned array indexing: (px, pz) -> index = (px) + (pz) * paddedWidth
     * where px=0..paddedW-1 corresponds to worldX = startX - radius + px.
     */
    private ForgeBiome[] precomputePaddedBiomeGrid(int startX, int startZ, long seed, int radius) {
        int paddedW = 16 + 2 * radius;
        ForgeBiome[] grid = new ForgeBiome[paddedW * paddedW];

        int baseX = startX - radius;
        int baseZ = startZ - radius;
        UnImpressedBiomeSource nmsSrc = (UnImpressedBiomeSource) biomeSource;
        for (int pz = 0; pz < paddedW; pz++) {
            for (int px = 0; px < paddedW; px++) {
                int worldX = baseX + px;
                int worldZ = baseZ + pz;
                ForgeBiome b = nmsSrc.getBiomeProvider().resolveBiome(worldX, 0, worldZ, seed);
                grid[px + pz * paddedW] = b;
            }
        }

        return grid;
    }

    /**
     * Optimized biome-aware blending that reads the paddedBiomeGrid (avoids repeated resolveBiome calls)
     * and caches per-(provider,chunk) chunkHeights arrays so getChunkHeights(...) is not repeatedly invoked.
     * <p>
     * chunkX,chunkZ are the target chunk indices (pos.x,pos.z).
     * startX,startZ are the minBlockX & minBlockZ of the chunk (pos.getMinBlockX/Z()).
     * <p>
     * paddedBiomeGrid must be the precomputePaddedBiomeGrid result for the same radius.
     */
    private int[] computeBiomeAwareBlendedHeightsOptimized(int chunkX, int chunkZ, int startX, int startZ,
                                                           ForgeBiome[] paddedBiomeGrid, int radius) {
        final int W = 16, H = 16;
        int paddedW = 16 + 2 * radius;
        int[] out = new int[W * H];

        double sigma = Math.max(1.0, radius / 2.0);
        int size = radius * 2 + 1;
        double[] distWeights = new double[size * size];
        for (int dz = -radius, i = 0; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++, i++) {
                double d2 = dx * dx + dz * dz;
                distWeights[i] = Math.exp(-d2 / (2.0 * sigma * sigma));
            }
        }

        // cache chunk heights per provider -> map(chunkKey -> int[])
        Map<ChunkHeightProvider, Map<Long, int[]>> heightsCache = new HashMap<>();

        int baseWorldX = startX - radius;
        int baseWorldZ = startZ - radius;

        for (int lz = 0; lz < H; lz++) {
            for (int lx = 0; lx < W; lx++) {
                int cellIdx = lx + lz * W;
                int worldX = startX + lx;
                int worldZ = startZ + lz;
                ForgeBiome centerBiome = paddedBiomeGrid[(lx + radius) + (lz + radius) * paddedW];

                double sum = 0.0;
                double weightSum = 0.0;
                int kIdx = 0;

                for (int sdz = -radius; sdz <= radius; sdz++) {
                    for (int sdx = -radius; sdx <= radius; sdx++, kIdx++) {
                        double distW = distWeights[kIdx];

                        int sampleWorldX = worldX + sdx;
                        int sampleWorldZ = worldZ + sdz;

                        int sampleLocalX = (sampleWorldX - baseWorldX); // absolute in padded grid
                        int sampleLocalZ = (sampleWorldZ - baseWorldZ);
                        if (sampleLocalX < 0 || sampleLocalX >= paddedW || sampleLocalZ < 0 || sampleLocalZ >= paddedW) {
                            continue; // out of padded grid (shouldn't normally happen)
                        }

                        ForgeBiome sampleBiome = paddedBiomeGrid[sampleLocalX + sampleLocalZ * paddedW];

                        // compute which chunk contains the sample (use floorDiv / floorMod consistently)
                        int sampleChunkX = Math.floorDiv(sampleWorldX, 16);
                        int sampleChunkZ = Math.floorDiv(sampleWorldZ, 16);
                        int sampleLocalXInChunk = Math.floorMod(sampleWorldX, 16);
                        int sampleLocalZInChunk = Math.floorMod(sampleWorldZ, 16);

                        // defensive sanity-check
                        if (sampleLocalXInChunk < 0 || sampleLocalXInChunk >= 16 || sampleLocalZInChunk < 0 || sampleLocalZInChunk >= 16) {
                            VSPEPlatformPlugin.platformLogger().warn(String.format("Invalid local-in-chunk coords: sampleWorld=(%s,%s), sampleLocalInChunk=(%s,%s), padded indices=(%s,%s), paddedW=%s",
                                    sampleWorldX, sampleWorldZ, sampleLocalXInChunk, sampleLocalZInChunk, sampleLocalX, sampleLocalZ, paddedW));
                            continue;
                        }

                        int sampleIdx = sampleLocalXInChunk + sampleLocalZInChunk * 16;

                        // provider for the sample biome
                        ChunkHeightProvider sampleProvider = getOrCreateProviderForBiome(sampleBiome);

                        // provider -> inner map
                        Map<Long, int[]> providerMap = heightsCache.computeIfAbsent(sampleProvider, k -> new HashMap<>());

                        // safer chunkKey composition (avoid ^ which may collide)
                        long chunkKey = (((long) sampleChunkX) << 32) | (sampleChunkZ & 0xffffffffL);

                        int[] sampleHeights = providerMap.get(chunkKey);
                        if (sampleHeights == null) {
                            sampleHeights = sampleProvider.getChunkHeights(sampleChunkX, sampleChunkZ);
                            if (sampleHeights == null) {
                                sampleHeights = new int[16 * 16];
                                Arrays.fill(sampleHeights, getMinY());
                            }
                            providerMap.put(chunkKey, sampleHeights);
                        }

                        // final defensive check before indexing
                        if (sampleIdx >= sampleHeights.length) {
                            VSPEPlatformPlugin.platformLogger().error(String.format("Sample index out of range when blending heights. sampleIdx=%s, sampleHeights.length=%s, sampleChunk=(%s,%s), sampleLocalInChunk=(%s,%s), sampleWorld=(%s,%s), chunkX=%s, chunkZ=%s",
                                    sampleIdx, sampleHeights.length,
                                    sampleChunkX, sampleChunkZ, sampleLocalXInChunk, sampleLocalZInChunk, sampleWorldX, sampleWorldZ, chunkX, chunkZ));
                            // fallback: skip this sample contribution (safer than throwing)
                            continue;
                        }

                        int sampleH = sampleHeights[sampleIdx];

                        double biomeW = biomeSimilarityWeight(centerBiome, sampleBiome);
                        double w = distW * biomeW;
                        sum += sampleH * w;
                        weightSum += w;
                    }
                }

                if (weightSum <= 0.0) {
                    ChunkHeightProvider localProvider = getOrCreateProviderForBiome(centerBiome);
                    int[] localHeights = localProvider.getChunkHeights(chunkX, chunkZ);
                    out[cellIdx] = (localHeights != null && cellIdx >= 0 && cellIdx < localHeights.length) ? localHeights[cellIdx] : getMinY();
                } else {
                    out[cellIdx] = (int) Math.round(sum / weightSum);
                }
            }
        }

        return out;
    }

    private enum GenStage {
        NONE,
        BIOMES_INIT,
        FILLING,
        FILLING_INIT,
        FEATURES,
        STRUCTURES,
        DONE,
        FAILED
    }

    private static final class GenerationDebug {
        volatile GenStage stage = GenStage.NONE;
        volatile long startTimeMs = System.currentTimeMillis();
        volatile long lastUpdateMs = startTimeMs;
        volatile String threadName = "";
        volatile boolean biomesFilled = false;
        volatile boolean structuresPlaced = false;
        volatile int topYMin = Integer.MAX_VALUE;
        volatile int topYMax = Integer.MIN_VALUE;
        volatile double topYAvg = 0.0;
        volatile int computedColumns = 0;
        volatile String exception = null;

        void touch() {
            lastUpdateMs = System.currentTimeMillis();
            threadName = Thread.currentThread().getName();
        }

        void setStage(GenStage s) {
            stage = s;
            touch();
        }

        void setBiomesFilled() {
            biomesFilled = true;
            touch();
        }

        void setStructuresPlaced(boolean v) {
            structuresPlaced = v;
            touch();
        }

        void setTopYStats(int[] topYs) {
            if (topYs == null || topYs.length == 0) {
                topYMin = Integer.MAX_VALUE;
                topYMax = Integer.MIN_VALUE;
                topYAvg = 0;
                computedColumns = 0;
                return;
            }
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
            long sum = 0L;
            for (int t : topYs) {
                if (t < min) min = t;
                if (t > max) max = t;
                sum += t;
            }
            topYMin = min;
            topYMax = max;
            computedColumns = topYs.length;
            topYAvg = (double) sum / topYs.length;
            touch();
        }

        void setException(Throwable t) {
            exception = t == null ? null : (t.getClass().getSimpleName() + ": " + t.getMessage());
            stage = GenStage.FAILED;
            touch();
        }

        // compact one-line status for debug screen
        String shortStatus(ChunkPos pos) {
            long now = System.currentTimeMillis();
            String ex = exception == null ? "" : " EX:" + exception;
            return String.format("chunk %d,%d stage=%s t=%dms biomes=%s %n    structs=%s topY[min=%d max=%d avg=%.1f cols=%d] thread=%s%s",
                    pos.x, pos.z, stage, lastUpdateMs, biomesFilled, structuresPlaced,
                    topYMin == Integer.MAX_VALUE ? -9999 : topYMin,
                    topYMax == Integer.MIN_VALUE ? -9999 : topYMax,
                    topYAvg, computedColumns, threadName, ex);
        }
    }
}