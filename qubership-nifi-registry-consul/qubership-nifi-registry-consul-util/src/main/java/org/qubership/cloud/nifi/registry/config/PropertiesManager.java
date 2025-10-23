package org.qubership.cloud.nifi.registry.config;

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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
public class PropertiesManager {
    private static final Logger LOG = LoggerFactory.getLogger(PropertiesManager.class);
    private static final String LOGGER_TAG = "logger";
    private static final String LOGGER_PREFIX = LOGGER_TAG + ".";
    private static final int LOGGER_PREFIX_LENGTH = LOGGER_PREFIX.length();
    @Value("classpath:logback-template.xml")
    private Resource sourceXmlFile;
    @Value("classpath:nifi_registry_default.properties")
    private Resource defaultPropertiesFile;
    @Value("classpath:nifi_registry_internal.properties")
    private Resource internalPropertiesFile;
    @Value("classpath:nifi_registry_internal_comments.properties")
    private Resource internalPropertiesCommentsFile;
    private Map<String, String> consulPropertiesMap = new HashMap<>();
    @Value("${config.file.path}")
    private String path;
    private ConfigurableEnvironment env;
    private Environment appEnv;

    /**
     * Default constructor for Spring.
     * @param configEnv ConfigurableEnvironment instance to use
     * @param applicationEnv application Environment instance to use
     */
    @Autowired
    public PropertiesManager(final ConfigurableEnvironment configEnv, final Environment applicationEnv) {
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
        readConsulProperties();
        buildPropertiesFile();
        buildLogbackXMLFile();
        LOG.info("nifi registry properties files generated");
    }

    private void buildLogbackXMLFile()
            throws ParserConfigurationException, IOException, SAXException, TransformerException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        //configure to avoid XXE attacks:
        dbFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        dbFactory.setXIncludeAware(false);
        dbFactory.setExpandEntityReferences(false);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = null;
        try (InputStream is = new BufferedInputStream(sourceXmlFile.getInputStream())) {
            doc = dBuilder.parse(is);
        }
        doc.getDocumentElement().normalize();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Root element: {}", doc.getDocumentElement().getNodeName());
        }
        NodeList loggersList = doc.getElementsByTagName(LOGGER_TAG);

        for (Map.Entry<String, String> consulEntry : consulPropertiesMap.entrySet()) {
            // if it starts with "logger.*" then, check element in logback.xml
            if (consulEntry.getKey().toLowerCase().startsWith(LOGGER_PREFIX)) {
                String xmlKey = consulEntry.getKey().substring(LOGGER_PREFIX_LENGTH);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("current xmlKey: {}", xmlKey);
                }
                addOrUpdateLogger(xmlKey, consulEntry.getValue(), loggersList, doc);
            }
        }
        try (OutputStream output = new BufferedOutputStream(new FileOutputStream(path + "logback.xml"))) {
            //write to new file:
            writeXml(doc, output);
        }
        LOG.info("logback.xml file created at path: {}", path);
    }

    private void addOrUpdateLogger(String loggerName, String loggerLevel, NodeList loggersList, Document doc) {
        boolean loggerElementFound = false;
        for (int i = 0; i < loggersList.getLength(); i++) {
            Node prop = loggersList.item(i);
            NamedNodeMap attr = prop.getAttributes();
            if (attr != null) {
                Node loggerKey = attr.getNamedItem("name");
                if (loggerName.equalsIgnoreCase(loggerKey.getNodeValue())) {
                    //key found then update element in xml file
                    attr.getNamedItem("level").setTextContent(loggerLevel);
                    loggerElementFound = true;
                    break;
                }
            }
        }
        if (!loggerElementFound) {
            // add new element to xml file
            Element newLogger = doc.createElement(LOGGER_TAG);
            newLogger.setAttribute("name", loggerName);
            newLogger.setAttribute("level", loggerLevel);
            Node firstLogNode = doc.getElementsByTagName(LOGGER_TAG).item(0);
            //insert before first node with tag=logger
            doc.getDocumentElement().insertBefore(newLogger, firstLogNode);
        }
    }

    private void writeXml(Document doc, OutputStream output) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        //configure to avoid XXE attacks:
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        Transformer transformer = transformerFactory.newTransformer();

        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(output);

        transformer.transform(source, result);
    }

    private void readConsulProperties() {
        MutablePropertySources srcs = env.getPropertySources();
        Set<String> allPropertyNames = new HashSet<>();
        for (PropertySource<?> src1 : srcs) {
            // get properties for ConsulPropertySource
            if (ConsulPropertySource.class.isAssignableFrom(src1.getClass())) {
                String[] allNames = ((ConsulPropertySource) src1).getPropertyNames();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("allNames array: {}", List.of(allNames));
                }
                addMatchingPropertyNames(allNames, allPropertyNames);
            }
        }
        LOG.debug("All property names = {}", allPropertyNames);
        for (String property : allPropertyNames) {
            consulPropertiesMap.put(property, appEnv.getProperty(property));
        }
        LOG.debug("consulPropertiesMap map: {}", consulPropertiesMap);
    }

    private static void addMatchingPropertyNames(String[] propertyNamesFromSource, Set<String> allPropertyNames) {
        for (String name : propertyNamesFromSource) {
            String lowercaseName = name.toLowerCase();
            //fetch only properties starting with logger.* and nifi.registry.*
            if (lowercaseName.startsWith(LOGGER_TAG) || lowercaseName.startsWith("nifi.registry")) {
                allPropertyNames.add(name);
            }
        }
    }

    /**
     * Builds the {@code nifi-registry.properties} file by combining default,
     * internal (unchangeable), and Consul-provided properties.
     * @throws IOException if an I/O error occurs while writing the properties file
     */
    public void buildPropertiesFile() throws IOException {
        String fileName = path + "nifi-registry.properties";

        //we have to build combinedNifiRegistryProperties properties map. copy nifiRegistryDefaultProps
        // as is without order change
        Map<String, String> combinedNifiRegistryProperties = getOrderedProperties(defaultPropertiesFile);

        //consul
        for (Map.Entry<String, String> consulEntry: consulPropertiesMap.entrySet()) {
            // if it starts with "nifi.registry.*", add in nifiRegistryProperties
            if (consulEntry.getKey().toLowerCase().startsWith("nifi.registry.")) {
                combinedNifiRegistryProperties.put(consulEntry.getKey(), consulEntry.getValue());
            }
        }

        //nifi_registry_internal properties should be placed as is, in the same order
        Map<String, String> nifiRegistryInternalProps = getOrderedProperties(internalPropertiesFile);
        combinedNifiRegistryProperties.putAll(nifiRegistryInternalProps);
        if (LOG.isDebugEnabled()) {
            LOG.debug("combined nifi registry Properties: {}", combinedNifiRegistryProperties);
        }

        // remove properties from combinedNifiRegistryProperties map
        // that are present in nifi_internal_comments.properties
        for (String s : READ_ONLY_NIFI_REGISTRY_PROPS) {
            combinedNifiRegistryProperties.remove(s);
        }

        //write nifiProperties to properties file
        try (PrintWriter pw = new PrintWriter(new FileOutputStream(fileName)); BufferedReader reader =
                new BufferedReader(new InputStreamReader(internalPropertiesCommentsFile.getInputStream()))) {
            //Storing the map in properties file in order
            for (Map.Entry<String, String> entry : combinedNifiRegistryProperties.entrySet()) {
                pw.print(entry.getKey());
                pw.print("=");
                pw.println(entry.getValue());
            }

            // store all commented properties from nifi_registry_internal_comments.properties in file
            String line = reader.readLine();
            while (line != null) {
                pw.println(line);
                // read next line
                line = reader.readLine();
            }
        }
        LOG.info("Nifi Registry Properties file created : {}", fileName);
    }

    /**
     * Loads properties from the given resource (file) and returns them as an ordered map,
     * preserving the order of the properties.
     *
     * @param rs the resource containing properties data
     * @return a {@code LinkedHashMap} of property names and values, in file order
     * @throws IOException if an I/O error occurs while reading the resource
     */
    public Map<String, String> getOrderedProperties(Resource rs) throws IOException {
        Map<String, String> mp = new LinkedHashMap<>();
        try (InputStream in = rs.getInputStream()) {
            (new Properties() {
                @Override
                public synchronized Object put(Object key, Object value) {
                    return mp.put((String) key, (String) value);
                }
            }).load(in);
        }
        return mp;
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
        readConsulProperties();
        try {
            buildLogbackXMLFile();
        } catch (Exception e) {
            LOG.error("Exception while processing change event from consul", e);
        }
    }
}
