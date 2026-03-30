package org.qubership.cloud.nifi.registry.quarkus.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.qubership.cloud.nifi.registry.config.common.BasePropertiesManager;
import org.qubership.cloud.nifi.registry.config.common.BasePropertiesManagerConfig;
import org.qubership.cloud.nifi.registry.config.common.PropertiesProvider;

import java.util.HashSet;
import java.util.Set;

/**
 * CDI producer for {@link BasePropertiesManager}.
 * <p>
 * Reads NiFi Registry configuration paths and resource names from MicroProfile Config
 * and produces an application-scoped {@link BasePropertiesManager} instance.
 */
@ApplicationScoped
public class BasePropertiesManagerProducer {
    @Inject
    @ConfigProperty(name = "nifi.registry.config.path")
    private String path;

    @Inject
    @ConfigProperty(name = "nifi.registry.config.logback.default", defaultValue = "logback-template.xml")
    private String defaultLogbackFile;

    @Inject
    @ConfigProperty(name = "nifi.registry.config.properties.default", defaultValue = "nifi_registry_default.properties")
    private String defaultPropertiesFile;

    @Inject
    @ConfigProperty(name = "nifi.registry.config.properties.internal",
            defaultValue = "nifi_registry_internal.properties")
    private String internalPropertiesFile;

    @Inject
    @ConfigProperty(name = "nifi.registry.config.properties.comments",
            defaultValue = "nifi_registry_internal_comments.properties")
    private String internalPropertiesCommentsFile;

    private static final Set<String> READ_ONLY_NIFI_REGISTRY_PROPS = new HashSet<>();
    static {
        READ_ONLY_NIFI_REGISTRY_PROPS.add("nifi.registry.security.identity.mapping.pattern.dn");
        READ_ONLY_NIFI_REGISTRY_PROPS.add("nifi.registry.security.identity.mapping.value.dn");
        READ_ONLY_NIFI_REGISTRY_PROPS.add("nifi.registry.security.identity.mapping.transform.dn");
    }

    /**
     * Produces a {@link BasePropertiesManager} instance configured with
     * NiFi Registry resource paths and the given properties provider.
     *
     * @param provider the properties provider for retrieving configuration values
     * @return configured {@link BasePropertiesManager} instance
     */
    @Produces
    public BasePropertiesManager basePropertiesManager(PropertiesProvider provider) {
        return new BasePropertiesManager(new BasePropertiesManagerConfig(
                defaultLogbackFile,
                defaultPropertiesFile,
                internalPropertiesFile,
                internalPropertiesCommentsFile,
                path,
                "nifi-registry.properties",
                "nifi.registry",
                READ_ONLY_NIFI_REGISTRY_PROPS,
                provider
        ));
    }

}
