package org.qubership.cloud.nifi.registry.quarkus.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.qubership.cloud.nifi.registry.config.common.PropertiesProvider;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link PropertiesProvider} implementation that retrieves configuration properties
 * from Consul via MicroProfile Config.
 */
@ApplicationScoped
public class ConsulPropertiesProvider
    implements PropertiesProvider {

    @Inject
    private Config config;
    /**
     * Gets all available property names from source.
     * @return set of property names
     */
    @Override
    public Set<String> getAllPropertyNamesFromSource() {
        Set<String> allPropertyNames = new HashSet<>();

        // Get all config property names from MicroProfile Config
        Iterable<String> propertyNames = config.getPropertyNames();
        for (String propertyName : propertyNames) {
            allPropertyNames.add(propertyName);
        }
        return allPropertyNames;
    }

    /**
     * Get property value with the specified name.
     * @param propertyName property name to get
     * @return property value
     */
    @Override
    public String getPropertyValue(String propertyName) {
        return config.getOptionalValue(propertyName, String.class).orElse(null);
    }
}
