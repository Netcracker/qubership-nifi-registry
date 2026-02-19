package org.qubership.cloud.nifi.registry.config.common;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Set;

public class BasePropertiesManagerTest {
    //TODO: add tests with different parameter values:
    private BasePropertiesManager pm = new BasePropertiesManager(new BasePropertiesManagerConfig(
            "logback-template.xml",
            "nifi_registry_default.properties",
            "nifi_registry_internal.properties",
            "nifi_registry_internal_comments.properties",
            "./conf/",
            "nifi-registry.properties",
            "nifi.registry",
            Set.of(""),
            new TestPropertiesProvider()
    ));

    @BeforeAll
    public static void init() {
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
        Assertions.assertTrue(logbackConfig.exists());
        File nifiRegistryPropsConfig = new File("./conf/nifi-registry.properties");
        Assertions.assertTrue(nifiRegistryPropsConfig.exists());
        Properties nifiRegistryProps = new Properties();
        try (InputStream in = new BufferedInputStream(new FileInputStream(nifiRegistryPropsConfig))) {
            nifiRegistryProps.load(in);
            Assertions.assertEquals("200", nifiRegistryProps.getProperty("nifi.registry.web.jetty.threads"));
            Assertions.assertEquals("15",
                    nifiRegistryProps.getProperty("nifi.registry.db.maxConnections"));
            Assertions.assertEquals("10 secs",
                    nifiRegistryProps.getProperty("nifi.registry.security.user.oidc.connect.timeout"));
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
