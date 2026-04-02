package org.qubership.cloud.nifi.registry.config;

import org.qubership.cloud.nifi.config.common.BasePropertiesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;

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
public class PropertiesManager {

    private static final Logger LOG = LoggerFactory.getLogger(PropertiesManager.class);
    @Autowired
    private BasePropertiesManager basePropertiesManager;
    //Not used, kept for backward compatibility:
    private ConfigurableEnvironment env;
    private Environment appEnv;

    //Not used, kept for backward compatibility.
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

    //Not used, kept for backward compatibility.
    /**
     * Default constructor.
     */
    public PropertiesManager() {
        // Default constructor
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
        this.basePropertiesManager.generateNifiPropertiesAndLogbackConfig();
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
            this.basePropertiesManager.updateLogbackConfig();
        } catch (Exception e) {
            LOG.error("Exception while processing change event from consul", e);
        }
    }
}
