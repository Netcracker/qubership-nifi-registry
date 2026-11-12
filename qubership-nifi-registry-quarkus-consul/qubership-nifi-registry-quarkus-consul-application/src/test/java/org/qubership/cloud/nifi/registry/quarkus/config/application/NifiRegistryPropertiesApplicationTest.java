package org.qubership.cloud.nifi.registry.quarkus.config.application;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.nifi.registry.quarkus.config.ConsulTestResource;
import org.qubership.cloud.nifi.registry.quarkus.config.InjectConsulContainer;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@QuarkusMainTest
@QuarkusTestResource(ConsulTestResource.class)
class NifiRegistryPropertiesApplicationTest {
    /**
     * Consul container instance.
     * Reserved for future development.
     */
    @InjectConsulContainer
    private ConsulContainer consul;

    @BeforeAll
    static void setup() {
        //prepare test directories:
        try {
            Files.createDirectories(Paths.get(".", "conf"));
            Files.createDirectories(Paths.get(".", "tmp"));
            // Remove output left by an aborted run, so the wait sees only files this run writes.
            Files.deleteIfExists(Paths.get(".", "conf", "nifi-registry.properties"));
            Files.deleteIfExists(Paths.get(".", "conf", "logback.xml"));
            Files.deleteIfExists(Paths.get(".", "tmp", "initial-config-completed.txt"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to prepare test dirs", e);
        }
    }

    private Thread mainThread;

    @Test
    void testPropertiesLoadOnStart(QuarkusMainLauncher launcher) {
        mainThread = new Thread(launcher::launch);
        mainThread.start();
        // The application writes the completion file after both config files.
        Path completionFile = Paths.get(".", "tmp", "initial-config-completed.txt");
        Awaitility.await("completion file " + completionFile)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> Files.exists(completionFile));
        Assertions.assertAll(
                () -> Assertions.assertTrue(Files.exists(Paths.get(".", "conf", "logback.xml")),
                        "conf/logback.xml exists"),
                () -> Assertions.assertTrue(Files.exists(Paths.get(".", "conf", "nifi-registry.properties")),
                        "conf/nifi-registry.properties exists"));
    }

    @AfterEach
    void cleanupThread() {
        if (mainThread != null) {
            mainThread.interrupt();
        }
    }

    @AfterAll
    static void tearDown() {
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
