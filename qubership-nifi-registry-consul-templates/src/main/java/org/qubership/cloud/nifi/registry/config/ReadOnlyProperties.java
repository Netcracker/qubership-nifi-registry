package org.qubership.cloud.nifi.registry.config;

import java.util.HashSet;
import java.util.Set;

/**
 * Class providing set of readonly properties for qubership-nifi-registry.
 */
public final class ReadOnlyProperties {
    private ReadOnlyProperties() {
    }

    /**
     * A set of properties that must not be configured from Configuration Source (e.g. Consul).
     */
    public static final Set<String> READ_ONLY_NIFI_REGISTRY_PROPERTIES = new HashSet<>();
    static {
        READ_ONLY_NIFI_REGISTRY_PROPERTIES.add("nifi.registry.security.identity.mapping.pattern.dn");
        READ_ONLY_NIFI_REGISTRY_PROPERTIES.add("nifi.registry.security.identity.mapping.value.dn");
        READ_ONLY_NIFI_REGISTRY_PROPERTIES.add("nifi.registry.security.identity.mapping.transform.dn");
    }
}
