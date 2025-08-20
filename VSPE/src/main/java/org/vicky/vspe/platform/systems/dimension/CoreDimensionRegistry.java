package org.vicky.vspe.platform.systems.dimension;

import org.vicky.vspe.platform.VSPEPlatformPlugin;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core-side registry for DimensionDescriptors.
 * Core modules can call CoreDimensionRegistry.register(...) during static init.
 * The platform should call installInto(plugin) during startup to consume registrations.
 */
public final class CoreDimensionRegistry {
    private static final ConcurrentHashMap<String, DimensionDescriptor> DESCRIPTORS = new ConcurrentHashMap<>();

    private CoreDimensionRegistry() {
    }

    /**
     * Called by core modules to register a dimension descriptor early (static init ok).
     */
    public static void register(DimensionDescriptor descriptor) {
        if (descriptor == null) throw new IllegalArgumentException("descriptor");
        String id = descriptor.identifier();
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("descriptor.id");
        // keep first-registered or replace? we keep first by default to avoid accidental override:
        DESCRIPTORS.putIfAbsent(id, descriptor);
    }

    /**
     * Convenience: register many
     */
    public static void registerAll(Collection<DimensionDescriptor> descriptors) {
        if (descriptors == null) return;
        for (DimensionDescriptor d : descriptors) register(d);
    }

    /**
     * Returns unmodifiable snapshot for discovery.
     */
    public static Collection<DimensionDescriptor> getRegisteredDescriptors() {
        return Collections.unmodifiableCollection(DESCRIPTORS.values());
    }

    /**
     * Install all currently-registered descriptors into the provided platform.
     * This is what the platform should call during startup (once).
     */
    public static void installInto(VSPEPlatformPlugin plugin) {
        if (plugin == null) throw new IllegalArgumentException("plugin");
        for (DimensionDescriptor d : getRegisteredDescriptors()) {
            try {
                plugin.registerDimensionDescriptor(d); // platform must implement this
            } catch (Exception ex) {
                // platform is allowed to log/handle. swallow here to avoid breaking installation loop.
                plugin.getPlatformLogger().error("Failed to register core descriptor: " + d.identifier(), ex);
            }
        }
    }

    /**
     * If platform is already running, let core ask platform to create a dimension immediately.
     */
    public static void registerAndCreateNow(DimensionDescriptor descriptor, VSPEPlatformPlugin plugin) {
        register(descriptor);
        if (plugin != null) {
            try {
                plugin.registerDimensionDescriptor(descriptor);
                plugin.processPendingDimensions();
            } catch (Exception ex) {
                plugin.getPlatformLogger().error("registerAndCreateNow failed for " + descriptor.identifier(), ex);
            }
        }
    }
}

