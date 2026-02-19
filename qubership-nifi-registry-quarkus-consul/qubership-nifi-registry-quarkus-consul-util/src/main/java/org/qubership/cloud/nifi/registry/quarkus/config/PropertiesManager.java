package org.qubership.cloud.nifi.registry.quarkus.config;

import org.qubership.cloud.nifi.registry.config.common.BasePropertiesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;

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
public class PropertiesManager {
    private static final Logger LOG = LoggerFactory.getLogger(PropertiesManager.class);

    @Inject
    private BasePropertiesManager basePropertiesManager;

    /**
     * Default constructor for CDI.
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
        this.basePropertiesManager.generateNifiRegistryProperties();
        LOG.info("nifi registry properties files generated");
    }
}
