package dk.kb.lookup.config;

import java.io.IOException;
import java.util.Arrays;

import dk.kb.lookup.api.ControlApi;
import dk.kb.lookup.api.LookupApi;
import dk.kb.lookup.api.StatusApi;
import dk.kb.lookup.api.impl.MemoryImpl;
import dk.kb.webservice.ContextListener;
import dk.kb.util.yaml.YAML;

/**
 * Sample configuration class using the Singleton pattern.
 * This should work well for most projects with non-dynamic properties.
 */
@SuppressWarnings("ALL")
public class ServiceConfig {

    public enum IMPLEMENTATION { memory;
        public static IMPLEMENTATION getDefault() {
            return memory;
        }
    }

    /**
     * Besides parsing of YAML files using SnakeYAML, the YAML helper class provides convenience
     * methods like {@code getInteger("someKey", defaultValue)} and {@code getSubMap("config.sub1.sub2")}.
     */
    private static YAML serviceConfig;

    /**
     * Initialized the configuration from the provided configFile.
     * This should normally be called from {@link ContextListener} as
     * part of web server initialization of the container.
     * @param configFile the configuration to load.
     * @throws IOException if the configuration could not be loaded or parsed.
     */
    public static synchronized void initialize(String configFile) throws IOException {
        serviceConfig = new YAML(configFile);
    }

    /**
     * Direct access to the backing YAML-class is used for configurations with more flexible content
     * and/or if the service developer prefers key-based property access.
     * @return the backing YAML-handler for the configuration.
     */
    public static YAML getConfig() {
        if (serviceConfig == null) {
            throw new IllegalStateException("The configuration should have been loaded, but was not");
        }
        return serviceConfig;
    }

    /**
     * @return the backing implementation for keeping track of the files.
     */
    public static IMPLEMENTATION getImplementation() {
        String implStr = getConfig().getString(".lookup.implementation", IMPLEMENTATION.getDefault().toString());
        try {
            return IMPLEMENTATION.valueOf(implStr);
        } catch (IllegalArgumentException e) {
            throw new UnsupportedOperationException(
                    "The implementation '" + implStr + "' is not known. " +
                    "Known implementations are " + Arrays.asList(IMPLEMENTATION.values()));
        }
    }

    public static Class<?> getImplementationClass() {
        switch (getImplementation()) {
            case memory: return MemoryImpl.class;
            default: throw new IllegalStateException("Inable to resolve implementation");
        }
    }

    public static LookupApi getLookup() {
        switch (getImplementation()) {
            case memory: return new MemoryImpl();
            default: throw new IllegalStateException("Inable to resolve implementation");
        }
    }

    public static ControlApi getControl() {
        switch (getImplementation()) {
            case memory: return new MemoryImpl();
            default: throw new IllegalStateException("Inable to resolve implementation");
        }
    }

    public static StatusApi getStatus() {
        switch (getImplementation()) {
            case memory: return new MemoryImpl();
            default: throw new IllegalStateException("Inable to resolve implementation");
        }
    }
}
