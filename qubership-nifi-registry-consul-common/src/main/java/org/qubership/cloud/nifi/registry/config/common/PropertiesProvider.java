package org.qubership.cloud.nifi.registry.config.common;

import java.util.Set;

public interface PropertiesProvider {
    /**
     * Gets all available property names from source.
     * @return set of property names
     */
    Set<String> getAllPropertyNamesFromSource();

    /**
     * Get property value with the specified name.
     * @param propertyName property name to get
     * @return property value
     */
    String getPropertyValue(String propertyName);
}
