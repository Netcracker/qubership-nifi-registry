package org.qubership.cloud.nifi.registry.config.spring;

import org.qubership.cloud.nifi.registry.config.common.BasePropertiesManager;
import org.qubership.cloud.nifi.registry.config.common.BasePropertiesManagerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

/**
 * Spring configuration class that produces a {@link BasePropertiesManager} bean
 * configured with NiFi Registry resource paths and the Consul properties provider.
 */
@Configuration
public class ConsulConfiguration {
    private static final Set<String> READ_ONLY_NIFI_REGISTRY_PROPS = new HashSet<>();

    static {
        READ_ONLY_NIFI_REGISTRY_PROPS.add("nifi.registry.security.identity.mapping.pattern.dn");
        READ_ONLY_NIFI_REGISTRY_PROPS.add("nifi.registry.security.identity.mapping.value.dn");
        READ_ONLY_NIFI_REGISTRY_PROPS.add("nifi.registry.security.identity.mapping.transform.dn");
    }

    /**
     * Creates a {@link BasePropertiesManager} bean configured with
     * NiFi Registry resource paths and the given Consul properties provider.
     *
     * @param defaultLogbackFile default logback XML template resource name
     * @param defaultPropertiesFile default properties template resource name
     * @param internalPropertiesFile internal (unchangeable) properties resource name
     * @param internalPropertiesCommentsFile internal properties comments resource name
     * @param path configuration file output path
     * @param propertiesProvider the Consul properties provider
     * @return configured {@link BasePropertiesManager} instance
     */
    @Bean
    public BasePropertiesManager basePropertiesManager(
            @Value("${nifi.registry.config.logback.default:logback-template.xml}") String defaultLogbackFile,
            @Value("${nifi.registry.config.properties.default:nifi_registry_default.properties}") String defaultPropertiesFile,
            @Value("${nifi.registry.config.properties.internal:nifi_registry_internal.properties}") String internalPropertiesFile,
            @Value("${nifi.registry.config.properties.comments:nifi_registry_internal_comments.properties}")
                String internalPropertiesCommentsFile,
            @Value("${config.file.path}") String path,
            ConsulPropertiesProvider propertiesProvider) {
        return new BasePropertiesManager(new BasePropertiesManagerConfig(
                defaultLogbackFile,
                defaultPropertiesFile,
                internalPropertiesFile,
                internalPropertiesCommentsFile,
                path,
                "nifi-registry.properties",
                "nifi.registry",
                READ_ONLY_NIFI_REGISTRY_PROPS,
                propertiesProvider
        ));
    }
}
