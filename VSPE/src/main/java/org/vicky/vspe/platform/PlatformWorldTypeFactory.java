package org.vicky.vspe.platform;

import org.vicky.vspe.platform.defaults.MultiBiomeWorldType;
import org.vicky.vspe.platform.defaults.SimpleWorldType;
import org.vicky.vspe.platform.defaults.SingleBiomeWorldType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Simple registry/factory for PlatformWorldType.
 *
 * Usage:
 *   PlatformWorldTypeFactory.register("frozen", FrozenWorldType::new);
 *   PlatformWorldType t = PlatformWorldTypeFactory.create("frozen");
 */
public final class PlatformWorldTypeFactory {
    // key -> supplier that returns a new instance (or a supplier of a singleton)
    private static final ConcurrentMap<String, Supplier<? extends PlatformWorldType>> REGISTRY = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Function<String, ? extends PlatformWorldType>> FACTORIES = new ConcurrentHashMap<>();

    private PlatformWorldTypeFactory() {}

    private static String normalize(String name) {
        return Objects.requireNonNull(name).trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Register a supplier that will produce instances for the given name.
     * Supplier can return a new instance each call, or a singleton (via () -> instance).
     */
    public static void register(String name, Supplier<? extends PlatformWorldType> supplier) {
        REGISTRY.put(normalize(name), Objects.requireNonNull(supplier));
    }

    /**
     * Register a supplier that will produce instances for the given name.
     * Supplier can return a new instance each call, or a singleton (via () -> instance).
     */
    public static void registerFactory(String name, Function<String, ? extends PlatformWorldType> factory) {
        FACTORIES.put(normalize(name), Objects.requireNonNull(factory));
    }

    /**
     * Register only if missing (safe for multiple modules attempting registration).
     * Returns true if registered.
     */
    public static boolean registerIfAbsent(String name, Supplier<? extends PlatformWorldType> supplier) {
        return REGISTRY.putIfAbsent(normalize(name), Objects.requireNonNull(supplier)) == null;
    }

    // Register a parameterized factory that accepts a single String (e.g. biomeName)
    public static boolean registerFactoryIfAbsent(String name, Function<String, ? extends PlatformWorldType> factory) {
        return FACTORIES.putIfAbsent(normalize(name), Objects.requireNonNull(factory)) == null;
    }

    /** Create a new instance (or the supplier's value). Throws if not found. */
    public static PlatformWorldType create(String name) {
        return createOptional(name).orElseThrow(() -> new NoSuchElementException("No Registered PlatformWorldType: " + name));
    }

    /** Create a new instance (or the supplier's value). Throws if not found. */
    public static PlatformWorldType createFactory(String name, String param) {
        return createOptionalFactory(name, param).orElseThrow(() -> new NoSuchElementException("No Factory PlatformWorldType: " + name));
    }

    /** Create optional (empty if not registered). */
    public static Optional<PlatformWorldType> createOptional(String name) {
        Supplier<? extends PlatformWorldType> supplier = REGISTRY.get(normalize(name));
        return supplier == null ? Optional.empty() : Optional.ofNullable(supplier.get());
    }

    /** Create optional (empty if not registered). */
    public static Optional<PlatformWorldType> createOptionalFactory(String name, String param) {
        Function<String, ? extends PlatformWorldType> supplier = FACTORIES.get(normalize(name));
        return supplier == null ? Optional.empty() : Optional.ofNullable(supplier.apply(param));
    }

    /** Check registration */
    public static boolean isRegistered(String name) {
        return REGISTRY.containsKey(normalize(name)) || FACTORIES.containsKey(normalize(name));
    }

    /** List available type names (unmodifiable) */
    public static Set<String> availableTypes() {
        var set = new HashSet<>(Set.copyOf(REGISTRY.keySet()));
        set.addAll(Set.copyOf(FACTORIES.keySet()));
        return Collections.unmodifiableSet(set);
    }

    /** Remove a registration (rarely used) */
    public static void unregister(String name) {
        REGISTRY.remove(normalize(name));
    }

    /** Register defaults (call during plugin init) */
    public static void registerDefaults() {
        register("NORMAL", (() -> new SimpleWorldType("OVERWORLD")));
        register("AMPLIFIED", (() -> new SimpleWorldType("NETHER")));
        register("SUPERFLAT", (() -> new SimpleWorldType("END")));
        registerFactory("SINGLEBIOME", (SingleBiomeWorldType::new));
        registerFactory("MULTIBIOME", (MultiBiomeWorldType::new));
    }

    /**
     * Discover implementations using ServiceLoader:
     * - Note: ServiceLoader usually gives instances, so we register suppliers that return that instance (singleton).
     * - If you want 'new instance per create', your service implementation should be a Factory that returns instances.
     */
    public static void loadFromServiceLoader() {
        ServiceLoader<PlatformWorldType> loader = ServiceLoader.load(PlatformWorldType.class);
        for (PlatformWorldType impl : loader) {
            String name = impl.name();
            // register the instance as a singleton supplier
            registerIfAbsent(name, () -> impl);
        }
    }
}