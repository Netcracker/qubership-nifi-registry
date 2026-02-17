package org.qubership.cloud.nifi.registry.quarkus.config;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.containers.Container;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * Quarkus test resource for managing Consul container lifecycle.
 */
public class ConsulTestResource implements QuarkusTestResourceLifecycleManager {

    private static final String CONSUL_IMAGE = "hashicorp/consul:1.20";
    private static final Logger LOG = LoggerFactory.getLogger(ConsulTestResource.class);
    private ConsulContainer consul;

    @Override
    public Map<String, String> start() {
        LOG.info("Starting Consul container for testing...");

        consul = new ConsulContainer(DockerImageName.parse(CONSUL_IMAGE))
                .withExposedPorts(8500);
        consul.setPortBindings(Collections.singletonList("18500:8500"));
        consul.start();

        LOG.info("Consul container started at localhost:18500");

        // Fill initial consul data
        populateConsulData();

        // Return empty map as we're using fixed port binding
        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        if (consul != null) {
            LOG.info("Stopping Consul container...");
            consul.stop();
        }
    }

    private void populateConsulData() {
        Container.ExecResult res = null;
        try {
            // Configure logging levels:
            res = consul.execInContainer(
                    "consul", "kv", "put", "config/local/application/logger.org.qubership", "DEBUG");
            LOG.debug("Result for put config/local/application/logger.org.qubership = {}", res.getStdout());
            assertSuccess(res, "Failed to set logger.org.qubership");

            res = consul.execInContainer(
                    "consul", "kv", "put",
                    "config/local/application/logger.org.apache.nifi.registry.StdErr", "INFO");
            LOG.debug("Result for put config/local/application/logger.org.apache.nifi.registry.StdErr = {}",
                    res.getStdout());
            assertSuccess(res, "Failed to set logger.org.apache.nifi.registry.StdErr");

            // Configure properties:
            // nifi.registry.db.maxConnections -- with default value
            res = consul.execInContainer(
                    "consul", "kv", "put", "config/local/application/nifi.registry.db.maxConnections", "15");
            LOG.debug("Result for put config/local/application/nifi.registry.db.maxConnections = {}", res.getStdout());
            assertSuccess(res, "Failed to set nifi.registry.db.maxConnections");

            // nifi.registry.security.user.oidc.connect.timeout -- w/o default value
            res = consul.execInContainer(
                    "consul", "kv", "put",
                    "config/local/application/nifi.registry.security.user.oidc.connect.timeout", "10 secs");
            LOG.debug("Result for put config/local/application/nifi.registry.security.user.oidc.connect.timeout = {}",
                    res.getStdout());
            assertSuccess(res, "Failed to set nifi.registry.security.user.oidc.connect.timeout");

            LOG.info("Consul data populated successfully");
        } catch (IOException | InterruptedException e) {
            if (res != null) {
                LOG.error("Last command stdout = {}", res.getStdout());
                LOG.error("Last command stderr = {}", res.getStderr());
            }
            LOG.error("Failed to fill initial consul data", e);
            throw new RuntimeException("Failed to populate Consul data", e);
        }
    }

    private void assertSuccess(Container.ExecResult res, String errorMessage) {
        if (res == null || res.getStdout() == null || !res.getStdout().contains("Success")) {
            throw new RuntimeException(errorMessage + ": " +
                (res != null ? "stdout=" + res.getStdout() + ", stderr=" + res.getStderr() : "null result"));
        }
    }
}
