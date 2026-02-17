package org.qubership.cloud.nifi.registry.quarkus.config;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

@QuarkusTest
@QuarkusTestResource(ConsulTestResource.class)
public class PropertiesManagerTest {

    private static final Logger LOG = LoggerFactory.getLogger(PropertiesManagerTest.class);

    @Inject
    PropertiesManager pm;

    @BeforeAll
    public static void setup() {
        //prepare test directories:
        try {
            Files.createDirectories(Paths.get(".", "conf"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create test dir", e);
        }
    }

    @Test
    public void testPropertiesLoadOnStart() throws Exception {
        pm.generateNifiRegistryProperties();
        File logbackConfig = new File("./conf/logback.xml");
        Assertions.assertTrue(logbackConfig.exists(), "logback.xml should exist");
        File nifiRegistryPropsConfig = new File("./conf/nifi-registry.properties");
        Assertions.assertTrue(nifiRegistryPropsConfig.exists(), "nifi-registry.properties should exist");
        Properties nifiRegistryProps = new Properties();
        try (InputStream in = new BufferedInputStream(new FileInputStream(nifiRegistryPropsConfig))) {
            nifiRegistryProps.load(in);
            Assertions.assertEquals("200", nifiRegistryProps.getProperty("nifi.registry.web.jetty.threads"),
                    "Default property value should be loaded");
            Assertions.assertEquals("15",
                    nifiRegistryProps.getProperty("nifi.registry.db.maxConnections"),
                    "Consul property should override default");
            Assertions.assertEquals("10 secs",
                    nifiRegistryProps.getProperty("nifi.registry.security.user.oidc.connect.timeout"),
                    "Consul property without default should be loaded");
        } catch (IOException e) {
            Assertions.fail("Failed to read nifi-registry.properties", e);
        }
    }

    @AfterAll
    public static void tearDown() {
        try {
            Files.deleteIfExists(Paths.get(".", "conf", "nifi-registry.properties"));
            Files.deleteIfExists(Paths.get(".", "conf", "logback.xml"));
            Files.deleteIfExists(Paths.get(".", "conf"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete conf test dir", e);
        }
    }
}
