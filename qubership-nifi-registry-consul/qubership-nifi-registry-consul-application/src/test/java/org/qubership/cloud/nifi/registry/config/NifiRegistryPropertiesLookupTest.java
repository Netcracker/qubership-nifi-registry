package org.qubership.cloud.nifi.registry.config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.consul.config.ConsulConfigAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.containers.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

@Testcontainers
@SpringBootTest(classes = {NifiRegistryPropertiesLookup.class})
@ImportAutoConfiguration(classes = {RefreshAutoConfiguration.class, ConsulConfigAutoConfiguration.class})
@ActiveProfiles("test")
public class NifiRegistryPropertiesLookupTest {
    private static final String CONSUL_IMAGE = "hashicorp/consul:1.20";
    private static final Logger LOG = LoggerFactory.getLogger(NifiRegistryPropertiesLookupTest.class);
    private static final ConsulContainer CONSUL;

    static {
        CONSUL = new ConsulContainer(DockerImageName.parse(CONSUL_IMAGE));
        CONSUL.start();
        System.setProperty("consul.test.port", String.valueOf(CONSUL.getMappedPort(8500)));
    }

    @BeforeAll
    public static void initContainer() {
        //fill initial consul data:
        //configure logging levels:
        putToConsul("config/local/application/logger.org.qubership", "DEBUG");
        putToConsul("config/local/application/logger.org.apache.nifi.registry.StdOut", "WARN");
        //configure properties:
        //nifi.registry.db.maxConnections -- with default value
        putToConsul("config/local/application/nifi/registry/db/maxConnections",
                "15");
        //nifi.registry.security.user.oidc.connect.timeout -- w/o default value
        putToConsul("config/local/application/nifi/registry/security/user/oidc/connect/timeout",
                "10 secs");
    }

    private static void putToConsul(String key, String value) {
        Container.ExecResult res = null;
        try {
            res = CONSUL.execInContainer(
                    "consul", "kv", "put", key, value);
            LOG.debug("Result for put key = {}: {}", key, res.getStdout());
            Assertions.assertTrue(res.getStdout() != null && res.getStdout().contains("Success"));
        } catch (IOException | InterruptedException e) {
            if (res != null) {
                LOG.error("Last command stdout = {}, stderr = {}", res.getStdout(), res.getStderr());
            }
            LOG.error("Failed to fill initial consul data", e);
            Assertions.fail("Failed to fill initial consul data", e);
        }
        //prepare test directories:
        try {
            Files.createDirectories(Paths.get(".", "conf"));
            Files.createDirectories(Paths.get(".", "tmp"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create test dir", e);
        }
    }

    @Test
    public void testPropertiesLoadOnStart() throws Exception {
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
        Assertions.assertTrue(Files.exists(Paths.get(".", "tmp", "initial-config-completed.txt")));
    }

    @AfterAll
    public static void tearDown() {
        System.clearProperty("consul.test.port");
        CONSUL.stop();
        try {
            Files.deleteIfExists(Paths.get(".", "conf", "nifi-registry.properties"));
            Files.deleteIfExists(Paths.get(".", "conf", "logback.xml"));
            Files.deleteIfExists(Paths.get(".", "conf"));
            Files.deleteIfExists(Paths.get(".", "tmp", "initial-config-completed.txt"));
            Files.deleteIfExists(Paths.get(".", "tmp"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete conf test dir", e);
        }
    }
}
