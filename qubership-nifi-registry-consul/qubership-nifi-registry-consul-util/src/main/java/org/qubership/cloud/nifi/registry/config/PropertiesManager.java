package org.qubership.cloud.nifi.registry.config;

import org.qubership.cloud.nifi.registry.config.util.PropertiesProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.consul.config.ConsulPropertySource;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The {@code PropertiesManager} is responsible for managing configuration properties
 * and logging settings for the Qubership NiFi Registry application.
 * <p>
 * <b>Responsibilities:</b>
 * <ul>
 *   <li>Accesses Consul to retrieve dynamic configuration properties.</li>
 *   <li>Generates {@code nifi-registry.properties} and {@code logback.xml} files before application startup.</li>
 *   <li>Watches for configuration changes and periodically updates {@code logback.xml}
 *   to support dynamic logging level changes.</li>
 * </ul>
 * <p>
 * This class is a Spring component with refresh scope, allowing it to respond to configuration changes at runtime.
 */
@Component
@RefreshScope
public class PropertiesManager
    implements PropertiesProvider {

    private static final Logger LOG = LoggerFactory.getLogger(PropertiesManager.class);
    @Value("classpath:logback-template.xml")
    private Resource sourceXmlFile;
    @Value("classpath:nifi_registry_default.properties")
    private Resource defaultPropertiesFile;
    @Value("classpath:nifi_registry_internal.properties")
    private Resource internalPropertiesFile;
    @Value("classpath:nifi_registry_internal_comments.properties")
    private Resource internalPropertiesCommentsFile;
    @Value("${config.file.path}")
    private String path;
    private ConfigurableEnvironment env;
    private Environment appEnv;
    private BasePropertiesManager basePropertiesManager;

    /**
     * Default constructor for Spring.
     * @param configEnv ConfigurableEnvironment instance to use
     * @param applicationEnv application Environment instance to use
     */
    @Autowired
    public PropertiesManager(final ConfigurableEnvironment configEnv,
                             final Environment applicationEnv) {
        this.env = configEnv;
        this.appEnv = applicationEnv;
    }

    private static final Set<String> READ_ONLY_NIFI_REGISTRY_PROPS = new HashSet<>();

    static {
        READ_ONLY_NIFI_REGISTRY_PROPS.add("nifi.registry.security.identity.mapping.pattern.dn");
        READ_ONLY_NIFI_REGISTRY_PROPS.add("nifi.registry.security.identity.mapping.value.dn");
        READ_ONLY_NIFI_REGISTRY_PROPS.add("nifi.registry.security.identity.mapping.transform.dn");
    }

    /**
     * Default constructor.
     */
    public PropertiesManager() {
        // Default constructor
    }

    private void initBasePropertiesManager() {
        if (this.basePropertiesManager == null) {
            try {
                this.basePropertiesManager = new BasePropertiesManager(
                        sourceXmlFile.getInputStream(),
                        defaultPropertiesFile.getInputStream(),
                        internalPropertiesFile.getInputStream(),
                        internalPropertiesCommentsFile.getInputStream(),
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
    }

    /**
     * Gets all available property names from Consul.
     * @return set of property names
     */
    @Override
    public Set<String> getAllPropertyNamesFromSource() {
        MutablePropertySources sources = env.getPropertySources();
        Set<String> allPropertyNames = new HashSet<>();
        for (PropertySource<?> src1 : sources) {
            // get properties for ConsulPropertySource
            if (ConsulPropertySource.class.isAssignableFrom(src1.getClass())) {
                String[] allNames = ((ConsulPropertySource) src1).getPropertyNames();
                Collections.addAll(allPropertyNames, allNames);
            }
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
        return appEnv.getProperty(propertyName);
    }

    /**
     * Handles environment change events by regenerating the {@code logback.xml} file
     * to support dynamic logging level changes.
     * <p>
     * This method is triggered automatically by Spring when configuration changes are detected in the environment.
     *
     * @param event the environment change event containing the changed property keys
     */
    @EventListener
    public void handleChangeEvent(EnvironmentChangeEvent event) {
        LOG.debug("Change event received for keys: {}", event.getKeys());
        try {
            this.basePropertiesManager.updateLogbackXML();
        } catch (Exception e) {
            LOG.error("Exception while processing change event from consul", e);
        }
    }
}
