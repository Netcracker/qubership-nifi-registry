package org.qubership.cloud.nifi.registry.quarkus.config;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.qubership.cloud.nifi.registry.config.BasePropertiesManager;
import org.qubership.cloud.nifi.registry.config.util.PropertiesProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * The {@code PropertiesManager} is responsible for managing configuration properties
 * and logging settings for the Qubership NiFi Registry application (Quarkus version).
 * <p>
 * <b>Responsibilities:</b>
 * <ul>
 *   <li>Accesses Consul to retrieve dynamic configuration properties.</li>
 *   <li>Generates {@code nifi-registry.properties} and {@code logback.xml} files before application startup.</li>
 * </ul>
 * <p>
 * This class is a CDI bean with application scope.
 */
@ApplicationScoped
public class PropertiesManager
        implements PropertiesProvider {
    private static final Logger LOG = LoggerFactory.getLogger(PropertiesManager.class);

    @Inject
    @ConfigProperty(name = "nifi.registry.config.path")
    private String path;

    @Inject
    private Config config;

    private BasePropertiesManager basePropertiesManager;

    private static final Set<String> READ_ONLY_NIFI_REGISTRY_PROPS = new HashSet<>();

    static {
        READ_ONLY_NIFI_REGISTRY_PROPS.add("nifi.registry.security.identity.mapping.pattern.dn");
        READ_ONLY_NIFI_REGISTRY_PROPS.add("nifi.registry.security.identity.mapping.value.dn");
        READ_ONLY_NIFI_REGISTRY_PROPS.add("nifi.registry.security.identity.mapping.transform.dn");
    }

    /**
     * Default constructor for CDI.
     */
    public PropertiesManager() {
        // Default constructor
    }

    private void initBasePropertiesManager() {
        if (this.basePropertiesManager == null) {
            try {
                this.basePropertiesManager = new BasePropertiesManager(
                        getResourceAsStream("logback-template.xml"),
                        getResourceAsStream("nifi_registry_default.properties"),
                        getResourceAsStream("nifi_registry_internal.properties"),
                        getResourceAsStream("nifi_registry_internal_comments.properties"),
                        path,
                        "nifi-registry.properties",
                        "nifi.registry",
                        READ_ONLY_NIFI_REGISTRY_PROPS,
                        this
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Generates the {@code nifi-registry.properties} and {@code logback.xml}
     * files using Consul data and default values.
     * <p>
     * This method performs the following steps:
     * <ol>
     *   <li>Reads properties from Consul and merges them with default and internal (unchangeable) properties.</li>
     *   <li>Builds the {@code nifi-registry.properties} file with the combined properties.</li>
     *   <li>Builds the {@code logback.xml} file, updating logger levels as specified in Consul.</li>
     * </ol>
     *
     * @throws IOException if an I/O error occurs while reading or writing files
     * @throws ParserConfigurationException if a configuration error occurs while parsing XML
     * @throws TransformerException if an error occurs during XML transformation
     * @throws SAXException if an error occurs while parsing XML
     */
    public void generateNifiRegistryProperties() throws IOException, ParserConfigurationException,
            TransformerException, SAXException {
        initBasePropertiesManager();
        this.basePropertiesManager.generateNifiRegistryProperties();
        LOG.info("nifi registry properties files generated");
    }

    /**
     * Get resource as input stream from classpath.
     *
     * @param resourceName the resource name
     * @return input stream for the resource
     * @throws IOException if resource not found
     */
    private InputStream getResourceAsStream(String resourceName) throws IOException {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
        if (is == null) {
            throw new IOException("Resource not found: " + resourceName);
        }
        return is;
    }

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
