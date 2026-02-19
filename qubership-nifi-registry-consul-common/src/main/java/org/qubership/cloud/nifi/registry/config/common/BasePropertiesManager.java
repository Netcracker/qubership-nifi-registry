package org.qubership.cloud.nifi.registry.config.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * The {@code BasePropertiesManager} is responsible for managing configuration properties
 * and logging settings for the qubership-nifi/qubership-nifi-registry.
 * <p>
 * <b>Responsibilities:</b>
 * <ul>
 *   <li>Accesses property source to retrieve dynamic configuration properties.</li>
 *   <li>Generates nifi properties and {@code logback.xml} files before application startup.</li>
 * </ul>
 * <p>
 */
public class BasePropertiesManager {
    private static final Logger LOG = LoggerFactory.getLogger(BasePropertiesManager.class);
    private static final String LOGGER_TAG = "logger";
    private static final String LOGGER_PREFIX = LOGGER_TAG + ".";
    private static final int LOGGER_PREFIX_LENGTH = LOGGER_PREFIX.length();

    private String defaultLogbackXmlResourceName;
    private String defaultPropertiesResourceName;
    private String internalPropertiesResourceName;
    private String internalPropertiesCommentsResourceName;
    private Map<String, String> consulPropertiesMap = new HashMap<>();
    private String configFilePath;
    private File configFile;
    private File logConfigFile;
    private String propertyPrefix;
    private Set<String> readonlyPropertyNames;
    private PropertiesProvider propertiesProvider;

    /**
     * Creates new instance of PropertiesManager.
     * @param defaultLogbackXmlResName
     * @param defaultPropertiesResName
     * @param internalPropertiesResName
     * @param internalPropertiesCommentsResName
     * @param confFilePath
     * @param confFileName
     * @param propPrefix
     * @param roPropertyNames
     * @param provider
     */
    public BasePropertiesManager(final String defaultLogbackXmlResName,
                                 final String defaultPropertiesResName,
                                 final String internalPropertiesResName,
                                 final String internalPropertiesCommentsResName,
                                 final String confFilePath,
                                 final String confFileName,
                                 final String propPrefix,
                                 final Set<String> roPropertyNames,
                                 final PropertiesProvider provider) {
        this.defaultLogbackXmlResourceName = defaultLogbackXmlResName;
        this.defaultPropertiesResourceName = defaultPropertiesResName;
        this.internalPropertiesResourceName = internalPropertiesResName;
        this.internalPropertiesCommentsResourceName = internalPropertiesCommentsResName;
        this.configFilePath = confFilePath;
        this.configFile = Paths.get(confFilePath, confFileName).toFile();
        this.logConfigFile = Paths.get(confFilePath, "logback.xml").toFile();
        if (propPrefix != null) {
            this.propertyPrefix = propPrefix.toLowerCase();
        } else {
            this.propertyPrefix = "";
        }
        this.readonlyPropertyNames = roPropertyNames;
        this.propertiesProvider = provider;
    }

    /**
     * Generates the nifi properties and {@code logback.xml}
     * files using Consul data and default values.
     * <p>
     * This method performs the following steps:
     * <ol>
     *   <li>Reads properties from Consul and merges them with default and internal (unchangeable) properties.</li>
     *   <li>Builds the properties file with the combined properties.</li>
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
        readAndFilterProperties();
        buildPropertiesFile();
        buildLogbackXMLFile();
        LOG.info("nifi properties and logback.xml files generated");
    }

    /**
     * Generates the {@code logback.xml}
     * file using Consul data and default values.
     * <p>
     * This method performs the following steps:
     * <ol>
     *   <li>Reads properties from Consul and merges them with default and internal (unchangeable) properties.</li>
     *   <li>Builds the {@code logback.xml} file, updating logger levels as specified in Consul.</li>
     * </ol>
     *
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws TransformerException
     * @throws SAXException
     */
    public void updateLogbackXML() throws IOException, ParserConfigurationException,
            TransformerException, SAXException {
        readAndFilterProperties();
        buildLogbackXMLFile();
        LOG.info("logback.xml file updated");
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
        try (InputStream is = new BufferedInputStream(getResourceAsStream(defaultLogbackXmlResourceName))) {
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
        try (OutputStream output = new BufferedOutputStream(new FileOutputStream(logConfigFile))) {
            //write to new file:
            writeXml(doc, output);
        }
        LOG.info("logback.xml file created at path: {}", configFilePath);
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

    /**
     * Reads properties names, filters them according to filtering rules and gets properties values.
     * Resulting map is saved as consulPropertiesMap.
     */
    protected void readAndFilterProperties() {
        Set<String> allPropertyNamesFromSource = propertiesProvider.getAllPropertyNamesFromSource();

        if (LOG.isDebugEnabled()) {
            LOG.debug("All property names from source: {}", allPropertyNamesFromSource);
        }
        Set<String> allPropertyNames = getMatchingPropertyNames(allPropertyNamesFromSource);
        if (LOG.isDebugEnabled()) {
            LOG.debug("All property names after filtering: {}", allPropertyNames);
        }
        for (String property : allPropertyNames) {
            consulPropertiesMap.put(property, propertiesProvider.getPropertyValue(property));
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("consulPropertiesMap with names and values: {}", consulPropertiesMap);
        }
    }

    private Set<String> getMatchingPropertyNames(Set<String> propertyNamesFromSource) {
        Set<String> allPropertyNames = new HashSet<>();
        for (String name : propertyNamesFromSource) {
            String lowercaseName = name.toLowerCase();
            //fetch only properties starting with logger.* or specified property prefix:
            if (lowercaseName.startsWith(LOGGER_TAG) || lowercaseName.startsWith(propertyPrefix)) {
                allPropertyNames.add(name);
            }
        }
        return allPropertyNames;
    }

    /**
     * Builds the {@code nifi-registry.properties} file by combining default,
     * internal (unchangeable), and Consul-provided properties.
     * @throws IOException if an I/O error occurs while writing the properties file
     */
    public void buildPropertiesFile() throws IOException {
        //We have to build combinedNifiProperties properties map.
        //First, copy nifi default properties as is without order change.
        Map<String, String> combinedNifiProperties = getOrderedProperties(
                getResourceAsStream(defaultPropertiesResourceName));

        //consul
        for (Map.Entry<String, String> consulEntry: consulPropertiesMap.entrySet()) {
            // if it starts with propertyPrefix, add it in nifiProperties
            if (consulEntry.getKey().toLowerCase().startsWith(propertyPrefix)) {
                combinedNifiProperties.put(consulEntry.getKey(), consulEntry.getValue());
            }
        }

        //internal properties should be placed as is, in the same order
        Map<String, String> nifiInternalProps = getOrderedProperties(
                getResourceAsStream(internalPropertiesResourceName));
        combinedNifiProperties.putAll(nifiInternalProps);
        if (LOG.isDebugEnabled()) {
            LOG.debug("combined nifi properties: {}", combinedNifiProperties);
        }

        // remove readonly properties from combinedNifiProperties map
        for (String s : readonlyPropertyNames) {
            combinedNifiProperties.remove(s);
        }

        //write nifiProperties to properties file
        try (PrintWriter pw = new PrintWriter(new FileOutputStream(configFile));
             BufferedReader reader =
                     new BufferedReader(new InputStreamReader(
                             getResourceAsStream(internalPropertiesCommentsResourceName)))) {
            //Storing the map in properties file in order
            for (Map.Entry<String, String> entry : combinedNifiProperties.entrySet()) {
                pw.print(entry.getKey());
                pw.print("=");
                pw.println(entry.getValue());
            }

            // add all commented properties from internal_comments.properties
            String line = reader.readLine();
            while (line != null) {
                pw.println(line);
                // read next line
                line = reader.readLine();
            }
        }
        LOG.info("NiFi Properties file created : {}", configFile.getPath());
    }

    /**
     * Extension of standard Properties class that holds a copy of loaded properties ordered with insertion order.
     */
    public static class OrderedProperties extends Properties {
        private final Map<String, String> orderedProperties = new LinkedHashMap<>();

        /**
         * Maps the specified key to the specified value in this hashtable. Neither the key nor the value can be null.
         * The value can be retrieved by calling the get method with a key that is equal to the original key.
         * @param key key with which the specified value is to be associated
         * @param value value to be associated with the specified key
         * @return the previous value of the specified key in this hashtable, or null if it did not have one
         */
        public synchronized Object put(Object key, Object value) {
            return orderedProperties.put((String) key, (String) value);
        }

        /**
         * Gets map with properties available in insertion order.
         * @return map with properties
         */
        public Map<String, String> getOrderedProperties() {
            return orderedProperties;
        }
    }

    /**
     * Loads properties from the given input stream and returns them as an ordered map,
     * preserving the order of the properties.
     *
     * @param in input stream containing properties data
     * @return a {@code Map} of property names and values, in file order
     * @throws IOException if an I/O error occurs while reading the resource
     */
    public Map<String, String> getOrderedProperties(InputStream in) throws IOException {
        OrderedProperties op = new OrderedProperties();
        op.load(in);
        return op.getOrderedProperties();
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
}
