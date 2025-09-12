package org.vicky.vspe_forge.forgeplatform;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.vicky.platform.PlatformPlayer;
import org.vicky.utilities.ContextLogger.ContextLogger;
import org.vicky.utilities.Identifiable;
import org.vicky.vspe.platform.systems.dimension.CoreDimensionRegistry;
import org.vicky.vspe.platform.systems.dimension.DimensionDescriptor;
import org.vicky.vspe.platform.systems.dimension.Exceptions.NoGeneratorException;
import org.vicky.vspe.platform.systems.dimension.Exceptions.WorldNotExistsException;
import org.vicky.vspe.platform.systems.dimension.PlatformBaseDimension;
import org.vicky.vspe.platform.systems.dimension.PlatformDimensionManager;
import org.vicky.vspe.platform.systems.dimension.globalDimensions.DimensionDescriptors;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.BiomeResolver;
import org.vicky.vspe.platform.utilities.Manager.EntityNotFoundException;
import org.vicky.vspe.platform.utilities.Manager.ManagerNotFoundException;
import org.vicky.vspe.platform.utilities.Manager.ManagerRegistry;
import org.vicky.vspe_forge.VspeForge;
import org.vicky.vspe_forge.dimension.*;

import java.text.Normalizer;
import java.util.*;

import static org.vicky.vspe_forge.dimension.ForgeDescriptorBasedDimension.stringToSeed;

public class ForgeDimensionManager implements PlatformDimensionManager<BlockState, Level> {
    public static final Map<String, UnImpressedChunkGenerator> GENERATORS = new HashMap<>();
    public static ForgeDimensionManager INSTANCE;
    public final List<PlatformBaseDimension<BlockState, Level>> LOADED_DIMENSIONS = new ArrayList<>();
    public final List<PlatformBaseDimension<BlockState, Level>> UNLOADED_DIMENSIONS = new ArrayList<>();
    public final Set<DimensionDescriptor> DIMENSION_DESCRIPTOR_SET = new HashSet<>();
    private final ContextLogger logger = new ContextLogger(ContextLogger.ContextType.SYSTEM, "DIMENSIONS");

    private ForgeDimensionManager() {
        ManagerRegistry.register(this);
    }

    public static ForgeDimensionManager getInstance() {
        if (INSTANCE == null) INSTANCE = new ForgeDimensionManager();
        return INSTANCE;
    }

    public static void prepareGenerators() {
        try {
            Class.forName("org.vicky.vspe.platform.systems.dimension.globalDimensions.DimensionDescriptors");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        CoreDimensionRegistry.getRegisteredDescriptors().forEach(descriptor -> {
            UnImpressedChunkGenerator gen = new UnImpressedChunkGenerator(new UnImpressedBiomeSource((BiomeResolver<ForgeBiome>) descriptor.resolver()), stringToSeed(descriptor.description()), descriptor);
            GENERATORS.put(cleanNamespace(descriptor.name()).toUpperCase(), gen);
        });
    }

    public static String cleanNamespace(String ns) {
        VspeForge.LOGGER.info("Cleaning name: {}", ns);
        if (ns == null) return "vspe";
        String s = Normalizer.normalize(ns, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
        s = s.replaceAll("\\s+", "");
        s = s.replaceAll("[^a-z0-9._\\-]", "");
        if (s.isEmpty()) return "vspe";
        if (s.length() > 64) s = s.substring(0, 64);
        return s;
    }

    @Override
    public List<PlatformBaseDimension<BlockState, Level>> getLoadedDimensions() {
        return LOADED_DIMENSIONS;
    }

    @Override
    public List<PlatformBaseDimension<BlockState, Level>> getUnLoadedDimensions() {
        return UNLOADED_DIMENSIONS;
    }

    public void processDimensionGenerators(boolean clean) {

    }

    public void processDimensions() {

    }

    @Override
    public PlatformBaseDimension<BlockState, Level> getPlayerDimension(PlatformPlayer platformPlayer) {
        return null;
    }

    @Override
    public Optional<PlatformBaseDimension<BlockState, Level>> getDimension(String dimensionId) {
        return Optional.empty();
    }

    @Override
    public String getManagerId() {
        return "dimension_manager";
    }

    @Override
    public void removeEntity(String namespace) throws EntityNotFoundException {
        Optional<ForgeBaseDimension> optional = LOADED_DIMENSIONS.stream().filter(ForgeBaseDimension.class::isInstance).map(ForgeBaseDimension.class::cast).filter(k -> k.getIdentifier().equals(namespace)).findAny();
        if (optional.isPresent()) {
            ForgeBaseDimension context = optional.get();
            context.deleteDimension();
        } else {
            throw new EntityNotFoundException("Failed to locate entity with id: " + namespace);
        }

    }

    @Override
    public void disableEntity(String namespace) throws EntityNotFoundException {
        Optional<ForgeBaseDimension> optional = LOADED_DIMENSIONS.stream().filter(ForgeBaseDimension.class::isInstance).map(ForgeBaseDimension.class::cast).filter(k -> k.getIdentifier().equals(namespace)).findAny();
        if (optional.isPresent()) {
            ForgeBaseDimension context = optional.get();
            context.disableDimension();
        } else {
            throw new EntityNotFoundException("Failed to locate entity with id: " + namespace);
        }
    }

    @Override
    public void enableEntity(String namespace) throws EntityNotFoundException {
        Optional<ForgeBaseDimension> optional = LOADED_DIMENSIONS.stream().filter(ForgeBaseDimension.class::isInstance).map(ForgeBaseDimension.class::cast).filter(k -> k.getIdentifier().equals(namespace)).findAny();
        if (optional.isPresent()) {
            ForgeBaseDimension context = optional.get();
            context.enableDimension();
        } else {
            throw new EntityNotFoundException("Failed to locate entity with id: " + namespace);
        }
    }

    @Override
    public void openDimensionsGUI(PlatformPlayer platformPlayer) {

    }

    @Override
    public List<Identifiable> getRegisteredEntities() {
        return new ArrayList<>(LOADED_DIMENSIONS);
    }

    @Override
    public List<Identifiable> getUnregisteredEntities() {
        return new ArrayList<>(LOADED_DIMENSIONS);
    }

    public void loadDimensionsFromDescriptors() {
        logger.print("Loading dimensions from descriptors", ContextLogger.LogType.PENDING);
        for (DimensionDescriptor descriptor : DIMENSION_DESCRIPTOR_SET) {
            try {
                var dimension = new ForgeDescriptorBasedDimension(descriptor, descriptor.description());
                logger.print("Created dimension from descriptor " + descriptor.name(), ContextLogger.LogType.SUCCESS);
                LOADED_DIMENSIONS.add(dimension);
            } catch (WorldNotExistsException | ManagerNotFoundException | NoGeneratorException e) {
                logger.print("Failed to create dimension from descriptor " + descriptor.name(), true);
                throw new RuntimeException(e);
            }
        }
    }

    public void registerGenerator(String name, UnImpressedChunkGenerator gen) {
        GENERATORS.put(name, gen);
    }
}
